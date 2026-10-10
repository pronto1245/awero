const { readdir, readFile } = require('node:fs/promises');
const path = require('node:path');
const { Pool } = require('pg');

async function migrate() {
  const connectionString = process.env.DATABASE_URL;
  if (!connectionString) {
    throw new Error('DATABASE_URL is required');
  }

  const pool = new Pool({ connectionString });
  const migrationsPath = path.resolve(__dirname, '../../../database/migrations');

  try {
    await pool.query(`
      CREATE TABLE IF NOT EXISTS schema_migrations (
        version text PRIMARY KEY,
        applied_at timestamptz NOT NULL DEFAULT now()
      )
    `);

    const migrationFiles = (await readdir(migrationsPath))
      .filter((file) => /^\d+_[a-z0-9_]+\.sql$/.test(file))
      .sort();

    for (const version of migrationFiles) {
      const client = await pool.connect();
      try {
        await client.query('BEGIN');
        await client.query('SELECT pg_advisory_xact_lock($1)', [724309811]);

        const applied = await client.query(
          'SELECT 1 FROM schema_migrations WHERE version = $1',
          [version],
        );
        if (applied.rowCount) {
          await client.query('COMMIT');
          continue;
        }

        const sql = await readFile(path.join(migrationsPath, version), 'utf8');
        await client.query(sql);
        await client.query('INSERT INTO schema_migrations(version) VALUES ($1)', [version]);
        await client.query('COMMIT');
        process.stdout.write(`Applied ${version}\n`);
      } catch (error) {
        await client.query('ROLLBACK');
        throw error;
      } finally {
        client.release();
      }
    }
  } finally {
    await pool.end();
  }
}

migrate().catch((error) => {
  process.stderr.write(`${error.message}\n`);
  process.exitCode = 1;
});
