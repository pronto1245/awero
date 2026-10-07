FROM node:22-alpine AS build
WORKDIR /app
COPY package.json pnpm-workspace.yaml ./
COPY apps/api/package.json apps/api/package.json
RUN corepack enable && pnpm install --filter @awero/api...
COPY . .
RUN pnpm --filter @awero/api build

FROM node:22-alpine
WORKDIR /app
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/apps/api/dist ./apps/api/dist
COPY --from=build /app/apps/api/package.json ./apps/api/package.json
CMD ["node","apps/api/dist/main.js"]
