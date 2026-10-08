const baseUrl = process.env.API_BASE_URL ?? 'http://127.0.0.1:3000/api/v1';

async function request(path, { method = 'GET', token, body } = {}) {
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: {
      ...(body ? { 'content-type': 'application/json' } : {}),
      ...(token ? { authorization: `Bearer ${token}` } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
  const data = await response.json();
  return { status: response.status, data };
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

async function main() {
  const health = await request('/health');
  assert(health.status === 200 && health.data.status === 'ok', 'health endpoint failed');

  const registration = await request('/auth/anonymous', {
    method: 'POST',
    body: {
      deviceId: 'awero-ci-device-one-0001',
      platform: 'IOS',
      appVersion: '0.1.0',
      timezone: 'Europe/Moscow',
    },
  });
  assert(registration.status === 201, 'anonymous registration failed');
  assert(typeof registration.data.accessToken === 'string', 'registration returned no bearer token');
  const token = registration.data.accessToken;

  const unauthorized = await request('/alarms');
  assert(unauthorized.status === 401, 'private route accepted a missing token');

  const invalidAlarm = await request('/alarms', {
    method: 'POST',
    token,
    body: { hour: 24, minute: 0 },
  });
  assert(invalidAlarm.status === 400, 'alarm validation did not reject an invalid hour');

  const created = await request('/alarms', {
    method: 'POST',
    token,
    body: { hour: 7, minute: 30, timezoneMode: 'FIXED', fixedTimezone: 'Europe/Moscow' },
  });
  assert(created.status === 201, 'alarm create failed');
  assert(created.data.item.version === 1, 'new alarm version was not initialized');
  const alarmId = created.data.item.id;

  const invalidTimezone = await request(`/alarms/${alarmId}`, {
    method: 'PATCH',
    token,
    body: { fixedTimezone: 'not-a-timezone' },
  });
  assert(invalidTimezone.status === 400, 'invalid timezone was accepted');

  const updated = await request(`/alarms/${alarmId}`, {
    method: 'PATCH',
    token,
    body: { minute: 45 },
  });
  assert(updated.status === 200, 'alarm update failed');
  assert(updated.data.item.version === 2 && updated.data.item.minute === 45, 'alarm version did not advance');

  const secondRegistration = await request('/auth/anonymous', {
    method: 'POST',
    body: {
      deviceId: 'awero-ci-device-two-0002',
      platform: 'ANDROID',
      appVersion: '0.1.0',
      timezone: 'UTC',
    },
  });
  const crossOwnerUpdate = await request(`/alarms/${alarmId}`, {
    method: 'PATCH',
    token: secondRegistration.data.accessToken,
    body: { minute: 10 },
  });
  assert(crossOwnerUpdate.status === 404, 'a different anonymous owner accessed the alarm');

  const removed = await request(`/alarms/${alarmId}`, { method: 'DELETE', token });
  assert(removed.status === 200 && removed.data.version === 3, 'alarm delete/tombstone failed');
  const list = await request('/alarms', { token });
  assert(list.status === 200 && list.data.items.length === 0, 'deleted alarm remained in the list');

  process.stdout.write('AWERO API anonymous auth + alarm CRUD: PASS\n');
}

main().catch((error) => {
  process.stderr.write(`${error.message}\n`);
  process.exitCode = 1;
});
