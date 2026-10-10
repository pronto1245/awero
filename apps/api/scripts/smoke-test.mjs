import { randomBytes, randomUUID } from 'node:crypto';

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
  const securityHeaders = await fetch(`${baseUrl}/health`);
  assert(securityHeaders.headers.get('x-content-type-options') === 'nosniff', 'security headers were not applied');
  assert(securityHeaders.headers.get('x-frame-options') === 'DENY', 'frame policy was not applied');
  assert(
    securityHeaders.headers.get('access-control-allow-origin') === null,
    'an unconfigured browser origin was allowed by CORS',
  );
  const deniedOrigin = await fetch(`${baseUrl}/health`, { headers: { origin: 'https://untrusted.example' } });
  assert(
    deniedOrigin.headers.get('access-control-allow-origin') === null,
    'an untrusted browser origin was allowed by CORS',
  );

  const installationSecret = randomBytes(32).toString('base64url');

  const registration = await request('/auth/anonymous', {
    method: 'POST',
    body: {
      deviceId: 'awero-ci-device-one-0001',
      installationSecret,
      platform: 'IOS',
      appVersion: '0.1.0',
      timezone: 'Europe/Moscow',
    },
  });
  assert(registration.status === 201, 'anonymous registration failed');
  assert(typeof registration.data.accessToken === 'string', 'registration returned no bearer token');
  assert(
    !('trial' in registration.data) && !('entitlement' in registration.data),
    'anonymous registration must never create or reset a trial entitlement',
  );
  const token = registration.data.accessToken;

  const sameInstallation = await request('/auth/anonymous', {
    method: 'POST',
    body: {
      deviceId: 'awero-ci-device-one-0001', installationSecret, platform: 'IOS',
      appVersion: '0.1.0', timezone: 'Europe/Moscow',
    },
  });
  assert(
    sameInstallation.status === 201 && sameInstallation.data.anonymousUserId === registration.data.anonymousUserId,
    'the same secure installation was issued a second anonymous identity',
  );
  const originalSessionStillWorks = await request('/alarms', { token });
  assert(originalSessionStillWorks.status === 200, 'registration revoked a valid account session');

  const unrelatedInstallationSecret = randomBytes(32).toString('base64url');
  const unrelatedInstallation = await request('/auth/anonymous', {
    method: 'POST',
    body: {
      deviceId: 'awero-ci-device-one-0001', installationSecret: unrelatedInstallationSecret,
      platform: 'ANDROID', appVersion: '0.1.0', timezone: 'Europe/Moscow',
    },
  });
  assert(
    unrelatedInstallation.status === 201 &&
      unrelatedInstallation.data.anonymousUserId !== registration.data.anonymousUserId,
    'a reported device ID was incorrectly treated as account ownership proof',
  );
  assert(
    !('trial' in unrelatedInstallation.data) && !('entitlement' in unrelatedInstallation.data),
    'a new anonymous identity for the same reported device must not mint trial entitlement',
  );
  const unrelatedToken = unrelatedInstallation.data.accessToken;
  const binding = await request('/auth/anonymous/credentials', {
    method: 'POST', token: unrelatedToken, body: { installationSecret: unrelatedInstallationSecret },
  });
  assert(binding.status === 201 && binding.data.bound, 'installation secret could not be bound to the authenticated legacy account');
  const alreadyBound = await request('/auth/anonymous/credentials', {
    method: 'POST', token, body: { installationSecret: unrelatedInstallationSecret },
  });
  assert(alreadyBound.status === 409, 'an installation secret was bound to a different account');

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
  assert(created.data.item.enabled === true && created.data.item.weekdays.length === 7, 'alarm defaults were incomplete');
  const alarmId = created.data.item.id;

  const invalidTimezone = await request(`/alarms/${alarmId}`, {
    method: 'PATCH',
    token,
    body: { fixedTimezone: 'not-a-timezone' },
  });
  assert(invalidTimezone.status === 400, 'invalid timezone was accepted');
  const unchanged = await request('/alarms', { token });
  assert(
    unchanged.data.items[0].version === 1 && unchanged.data.items[0].fixedTimezone === 'Europe/Moscow',
    'rejected alarm update changed persisted state',
  );

  const emptyUpdate = await request(`/alarms/${alarmId}`, { method: 'PATCH', token, body: {} });
  assert(emptyUpdate.status === 400, 'empty alarm update was accepted');

  const updated = await request(`/alarms/${alarmId}`, {
    method: 'PATCH',
    token,
    body: { minute: 45 },
  });
  assert(updated.status === 200, 'alarm update failed');
  assert(updated.data.item.version === 2 && updated.data.item.minute === 45, 'alarm version did not advance');

  const qrAlarm = await request('/alarms', {
    method: 'POST', token,
    body: { hour: 8, minute: 15, missionType: 'QR', qrExpectedCode: 'setup-code-456' },
  });
  assert(qrAlarm.status === 201 && qrAlarm.data.item.qrExpectedCode === 'setup-code-456', 'QR alarm create lost its expected code');
  const qrAlarmUpdate = await request(`/alarms/${qrAlarm.data.item.id}`, {
    method: 'PATCH', token, body: { qrExpectedCode: 'updated-setup-code' },
  });
  assert(qrAlarmUpdate.status === 200 && qrAlarmUpdate.data.item.qrExpectedCode === 'updated-setup-code', 'QR alarm update lost its expected code');
  const oversizedQrCode = await request(`/alarms/${qrAlarm.data.item.id}`, {
    method: 'PATCH', token, body: { qrExpectedCode: 'x'.repeat(2049) },
  });
  assert(oversizedQrCode.status === 400, 'oversized QR expected code was accepted');
  const deletedQrAlarm = await request(`/alarms/${qrAlarm.data.item.id}`, { method: 'DELETE', token });
  assert(deletedQrAlarm.status === 200, 'QR smoke-test alarm cleanup failed');
  const serverAlarmSnapshot = await request('/sync/alarms', { token });
  assert(
    serverAlarmSnapshot.status === 200 &&
      serverAlarmSnapshot.data.items.some((item) => item.id === alarmId && item.version === 2) &&
      serverAlarmSnapshot.data.items.some((item) => item.id === qrAlarm.data.item.id && item.status === 'DELETED'),
    'inbound alarm sync did not include current versions and deletion tombstones',
  );

  const sessionId = randomUUID();
  const triggerEventId = randomUUID();
  const wakeStartedAt = new Date(Date.now() - 20_000);
  const wakeStartedAtIso = wakeStartedAt.toISOString();
  const sessionBody = {
    id: sessionId,
    alarmId,
    alarmVersion: 1,
    scheduledAt: wakeStartedAtIso,
    triggeredAt: wakeStartedAtIso,
    missionType: 'MATH',
    eventId: triggerEventId,
  };
  const futureSchedule = await request('/wake-sessions', {
    method: 'POST', token,
    body: {
      ...sessionBody,
      id: randomUUID(), eventId: randomUUID(),
      scheduledAt: new Date(Date.now() + 49 * 60 * 60 * 1000).toISOString(),
    },
  });
  assert(futureSchedule.status === 400, 'wake session accepted a schedule more than 48 hours in the future');
  const sessionCreated = await request('/wake-sessions', { method: 'POST', token, body: sessionBody });
  assert(sessionCreated.status === 201, 'wake session create failed');
  assert(sessionCreated.data.item.id === sessionId, 'wake session id was not retained');
  const duplicateSession = await request('/wake-sessions', { method: 'POST', token, body: sessionBody });
  assert(duplicateSession.status === 201 && duplicateSession.data.duplicate, 'wake session create was not idempotent');
  const changedSessionRetry = await request('/wake-sessions', {
    method: 'POST', token, body: { ...sessionBody, missionType: 'QR' },
  });
  assert(changedSessionRetry.status === 409, 'wake session ID accepted changed mission content');
  let wakeEventTime = Date.now() + 1_000;
  const postWakeEvent = (eventType, payload) => request(`/wake-sessions/${sessionId}/events`, {
    method: 'POST',
    token,
    body: {
      id: randomUUID(),
      eventType,
      occurredAt: new Date(wakeEventTime++).toISOString(),
      ...(payload ? { payload } : {}),
    },
  });
  const awakeBody = { id: randomUUID(), eventType: 'AWAKE' };
  const futureWakeEvent = await request(`/wake-sessions/${sessionId}/events`, {
    method: 'POST', token,
    body: { id: randomUUID(), eventType: 'AWAKE', occurredAt: new Date(Date.now() + 6 * 60 * 1000).toISOString() },
  });
  assert(futureWakeEvent.status === 400, 'wake event accepted a timestamp more than 5 minutes in the future');
  const awake = await request(`/wake-sessions/${sessionId}/events`, { method: 'POST', token, body: awakeBody });
  assert(awake.status === 201 && !awake.data.item.duplicate, 'wake event without a timestamp failed');
  const awakeRetry = await request(`/wake-sessions/${sessionId}/events`, { method: 'POST', token, body: awakeBody });
  assert(awakeRetry.status === 201 && awakeRetry.data.item.duplicate, 'wake event retry without a timestamp was not idempotent');

  const prematureComplete = await request(`/wake-sessions/${sessionId}/events`, {
    method: 'POST', token, body: { id: randomUUID(), eventType: 'COMPLETED' },
  });
  assert(prematureComplete.status === 409, 'wake session completed before a mission started');

  const startedBody = {
    id: randomUUID(),
    eventType: 'MISSION_STARTED',
    occurredAt: new Date(wakeEventTime++).toISOString(),
  };
  const started = await request(`/wake-sessions/${sessionId}/events`, { method: 'POST', token, body: startedBody });
  assert(started.status === 201 && !started.data.item.duplicate, 'mission start event failed');
  const duplicateStarted = await request(`/wake-sessions/${sessionId}/events`, {
    method: 'POST', token, body: startedBody,
  });
  assert(duplicateStarted.status === 201 && duplicateStarted.data.item.duplicate, 'wake event was not idempotent');
  const changedEventRetry = await request(`/wake-sessions/${sessionId}/events`, {
    method: 'POST', token,
    body: { ...startedBody, occurredAt: new Date(wakeEventTime++).toISOString() },
  });
  assert(changedEventRetry.status === 409, 'wake event ID accepted a changed timestamp');
  const snooze = await postWakeEvent('SNOOZE', { count: 1 });
  assert(snooze.status === 201, 'wake session snooze event failed');
  const missionFailed = await postWakeEvent('MISSION_FAILED');
  assert(missionFailed.status === 201, 'wake session mission failure event failed');
  const fallback = await postWakeEvent('FALLBACK');
  assert(fallback.status === 201, 'wake session fallback event failed');
  const completed = await postWakeEvent('COMPLETED');
  assert(completed.status === 201, 'wake session completion event failed');
  const sessionEvents = await request(`/wake-sessions/${sessionId}/events`, { token });
  assert(
    sessionEvents.status === 200 && sessionEvents.data.items.map((event) => event.eventType).join(',') ===
      'TRIGGERED,AWAKE,MISSION_STARTED,SNOOZE,MISSION_FAILED,FALLBACK,COMPLETED',
    'wake session event history was incomplete or out of order',
  );
  const sessions = await request('/wake-sessions', { token });
  assert(
    sessions.status === 200 && sessions.data.items[0].result === 'COMPLETED' && sessions.data.items[0].fallbackUsed,
    'wake session summary did not reflect its lifecycle',
  );
  const statistics = await request('/statistics/summary', { token });
  assert(
    statistics.status === 200 &&
      statistics.data.summary.totalWakes === 1 &&
      statistics.data.summary.successfulWakes === 1 &&
      statistics.data.summary.successRatePercent === 100 &&
      statistics.data.summary.snoozedWakes === 1 &&
      statistics.data.streak.current === 1 &&
      statistics.data.streak.best === 1,
    'statistics summary did not reflect the completed wake session',
  );

  const syncOperation = {
    id: randomUUID(),
    operationType: 'UPSERT',
    entityType: 'ALARM',
    entityId: alarmId,
    clientVersion: 2,
    payload: { minute: 50, weekdays: [1, 2, 3] },
    occurredAt: Date.now(),
  };
  const sync = await request('/sync', { method: 'POST', token, body: { operations: [syncOperation] } });
  assert(
    sync.status === 201 && sync.data.accepted === 1 && sync.data.acceptedIds[0] === syncOperation.id,
    'offline sync operation was not applied',
  );
  const syncedAlarm = await request('/alarms', { token });
  const syncedItem = syncedAlarm.data.items.find((item) => item.id === alarmId);
  assert(syncedItem.version === 3 && syncedItem.minute === 50 && syncedItem.weekdays.join(',') === '1,2,3', 'sync did not reconcile the alarm');
  const syncRetry = await request('/sync', { method: 'POST', token, body: { operations: [syncOperation] } });
  assert(syncRetry.status === 201 && syncRetry.data.accepted === 1, 'sync retry was not idempotently acknowledged');
  const afterRetry = await request('/alarms', { token });
  assert(afterRetry.data.items.find((item) => item.id === alarmId).version === 3, 'sync retry applied the alarm twice');
  const staleSyncOperation = {
    id: randomUUID(), operationType: 'UPDATE_ALARM', entityType: 'ALARM', entityId: alarmId,
    clientVersion: 2, payload: { minute: 5 }, occurredAt: Date.now(),
  };
  const staleSync = await request('/sync', { method: 'POST', token, body: { operations: [staleSyncOperation] } });
  assert(
    staleSync.status === 201 && staleSync.data.accepted === 0 && staleSync.data.conflicts[0].code === 'VERSION_MISMATCH' &&
      staleSync.data.conflicts[0].serverVersion === 3 && staleSync.data.conflicts[0].serverEntity.minute === 50,
    'stale sync did not return the current server alarm as a conflict',
  );
  const staleRetry = await request('/sync', { method: 'POST', token, body: { operations: [staleSyncOperation] } });
  assert(staleRetry.data.conflicts[0].code === 'VERSION_MISMATCH', 'conflict retry did not return its stable result');
  const conflictingSync = await request('/sync', {
    method: 'POST', token, body: { operations: [{ ...syncOperation, payload: { minute: 5 } }] },
  });
  assert(
    conflictingSync.status === 201 && conflictingSync.data.rejected[0].code === 'SYNC_OPERATION_ID_CONFLICT',
    'reused sync operation ID accepted different content or blocked its batch',
  );

  const independentSyncAlarmA = randomUUID();
  const independentSyncAlarmB = randomUUID();
  const invalidSyncOperation = {
    id: randomUUID(), operationType: 'UPSERT', entityType: 'ALARM', entityId: randomUUID(),
    clientVersion: 1, payload: { hour: 88, minute: 0 }, occurredAt: Date.now(),
  };
  const batchWithOneInvalidOperation = await request('/sync', {
    method: 'POST', token, body: { operations: [
      { id: randomUUID(), operationType: 'CREATE_ALARM', entityType: 'ALARM', entityId: independentSyncAlarmA,
        clientVersion: 1, payload: { hour: 6, minute: 10 }, occurredAt: Date.now() },
      invalidSyncOperation,
      { id: randomUUID(), operationType: 'CREATE_ALARM', entityType: 'ALARM', entityId: independentSyncAlarmB,
        clientVersion: 1, payload: { hour: 6, minute: 20 }, occurredAt: Date.now() },
    ] },
  });
  assert(
    batchWithOneInvalidOperation.status === 201 && batchWithOneInvalidOperation.data.accepted === 2 &&
      batchWithOneInvalidOperation.data.rejected.length === 1,
    'one invalid sync operation blocked valid operations in the same batch',
  );
  const rejectedRetry = await request('/sync', {
    method: 'POST', token, body: { operations: [invalidSyncOperation] },
  });
  assert(
    rejectedRetry.status === 201 && rejectedRetry.data.rejected[0].code === batchWithOneInvalidOperation.data.rejected[0].code,
    'a rejected sync operation did not return its stable result on retry',
  );
  const futureSyncOperation = {
    id: randomUUID(), operationType: 'UPSERT', entityType: 'ALARM', entityId: randomUUID(),
    clientVersion: 1, payload: { hour: 8, minute: 0 },
    occurredAt: new Date(Date.now() + 6 * 60 * 1000).toISOString(),
  };
  const futureSync = await request('/sync', {
    method: 'POST', token, body: { operations: [futureSyncOperation] },
  });
  assert(
    futureSync.status === 201 && futureSync.data.rejected[0].code === 'SYNC_OCCURRED_AT_OUT_OF_RANGE',
    'sync accepted an operation timestamp more than 5 minutes in the future',
  );

  const foreignAlarmSync = await request('/sync', {
    method: 'POST', token: unrelatedToken, body: { operations: [{
      id: randomUUID(), operationType: 'CREATE_ALARM', entityType: 'ALARM', entityId: alarmId,
      clientVersion: 1, payload: { hour: 8, minute: 0 }, occurredAt: Date.now(),
    }] },
  });
  assert(
    foreignAlarmSync.status === 201 && foreignAlarmSync.data.rejected[0].code === 'ALARM_ID_OWNERSHIP_CONFLICT',
    'foreign alarm UUID caused a server error instead of a safe ownership rejection',
  );

  const syncedAlarmId = randomUUID();
  const syncCreate = {
    id: randomUUID(), operationType: 'CREATE_ALARM', entityType: 'ALARM', entityId: syncedAlarmId,
    clientVersion: 1, payload: {
      label: 'Offline QR alarm', hour: 6, minute: 15, enabled: true, weekdays: [1, 3, 5],
      missionType: 'QR', qrExpectedCode: 'wake-code-123',
    },
    occurredAt: Date.now(),
  };
  const syncCreated = await request('/sync', { method: 'POST', token, body: { operations: [syncCreate] } });
  assert(syncCreated.status === 201 && syncCreated.data.accepted === 1, 'offline alarm create was not applied');
  const createdFromSync = await request('/alarms', { token });
  assert(
    createdFromSync.data.items.some((item) =>
      item.id === syncedAlarmId && item.hour === 6 && item.missionType === 'QR' && item.qrExpectedCode === 'wake-code-123'),
    'synced QR alarm settings were not preserved',
  );
  const syncUpdate = {
    id: randomUUID(), operationType: 'UPDATE_ALARM', entityType: 'ALARM', entityId: syncedAlarmId,
    clientVersion: 1, payload: { minute: 20, qrExpectedCode: 'updated-wake-code' },
  };
  const syncUpdated = await request('/sync', { method: 'POST', token, body: { operations: [syncUpdate] } });
  assert(syncUpdated.data.accepted === 1, 'offline alarm update was not applied');
  const syncUpdateRetry = await request('/sync', { method: 'POST', token, body: { operations: [syncUpdate] } });
  assert(syncUpdateRetry.data.accepted === 1, 'offline alarm update retry without a timestamp was not idempotent');
  const verifiedSyncUpdate = await request('/alarms', { token });
  const syncedQrUpdate = verifiedSyncUpdate.data.items.find((item) => item.id === syncedAlarmId);
  assert(
    syncedQrUpdate.version === 2 && syncedQrUpdate.qrExpectedCode === 'updated-wake-code',
    'timestamp-less sync retry applied twice or lost the QR code update',
  );
  const syncDelete = {
    id: randomUUID(), operationType: 'DELETE_ALARM', entityType: 'ALARM', entityId: syncedAlarmId,
    clientVersion: 2, payload: {}, occurredAt: Date.now(),
  };
  const syncDeleted = await request('/sync', { method: 'POST', token, body: { operations: [syncDelete] } });
  assert(syncDeleted.data.accepted === 1, 'offline alarm delete was not applied');
  const afterSyncDelete = await request('/alarms', { token });
  assert(!afterSyncDelete.data.items.some((item) => item.id === syncedAlarmId), 'synced delete did not create a tombstone');
  const unsupportedSync = await request('/sync', {
    method: 'POST', token,
    body: { operations: [{ id: randomUUID(), operationType: 'UPDATE', entityType: 'PROFILE', entityId: randomUUID(), payload: {} }] },
  });
  assert(
    unsupportedSync.status === 201 && unsupportedSync.data.rejected[0].code === 'UNSUPPORTED_SYNC_ENTITY',
    'one unsupported sync entity blocked the batch instead of becoming a per-operation rejection',
  );

  const analyticsEvent = {
    id: randomUUID(),
    eventName: 'alarm.updated',
    eventVersion: 1,
    properties: { source: 'smoke-test', alarmId },
  };
  const futureAnalytics = await request('/analytics/events', {
    method: 'POST', token,
    body: { events: [{ ...analyticsEvent, id: randomUUID(), occurredAt: new Date(Date.now() + 6 * 60 * 1000).toISOString() }] },
  });
  assert(
    futureAnalytics.status === 201 && futureAnalytics.data.rejected[0].code === 'ANALYTICS_OCCURRED_AT_OUT_OF_RANGE',
    'analytics accepted an event timestamp more than 5 minutes in the future',
  );
  const analytics = await request('/analytics/events', {
    method: 'POST', token, body: { events: [analyticsEvent] },
  });
  assert(
    analytics.status === 201 && analytics.data.accepted === 1 && analytics.data.acceptedIds[0] === analyticsEvent.id,
    'analytics event was not ingested',
  );
  const analyticsRetry = await request('/analytics/events', {
    method: 'POST', token, body: { events: [analyticsEvent] },
  });
  assert(analyticsRetry.status === 201 && analyticsRetry.data.accepted === 1, 'analytics retry was not idempotent');
  const conflictingAnalytics = await request('/analytics/events', {
    method: 'POST', token, body: { events: [{ ...analyticsEvent, properties: { source: 'changed' } }] },
  });
  assert(
    conflictingAnalytics.status === 201 && conflictingAnalytics.data.rejected[0].code === 'ANALYTICS_EVENT_ID_CONFLICT',
    'analytics event ID accepted changed properties or blocked the batch',
  );
  const isolatedAnalytics = await request('/analytics/events', {
    method: 'POST', token, body: { events: [
      { ...analyticsEvent, id: randomUUID(), properties: { source: 'valid-before' } },
      { ...analyticsEvent, id: randomUUID(), properties: { source: 'x'.repeat(20_000) } },
      { ...analyticsEvent, id: randomUUID(), properties: { source: 'valid-after' } },
    ] },
  });
  assert(
    isolatedAnalytics.status === 201 && isolatedAnalytics.data.accepted === 2 && isolatedAnalytics.data.rejected.length === 1,
    'one invalid analytics event blocked valid events in the same batch',
  );

  const supportTicket = {
    id: randomUUID(),
    category: 'ALARM',
    diagnostics: {
      appVersion: '0.1.0',
      platform: 'IOS',
      timezone: 'Europe/Moscow',
      alarmId,
      alarmVersion: 3,
      errorCode: 'SCHEDULE_MISSING',
      pendingSyncCount: 2,
    },
  };
  const support = await request('/support/diagnostics', { method: 'POST', token, body: supportTicket });
  assert(support.status === 201 && support.data.item.status === 'OPEN', 'support diagnostics ticket was not created');
  const supportRetry = await request('/support/diagnostics', { method: 'POST', token, body: supportTicket });
  assert(supportRetry.status === 201 && supportRetry.data.duplicate, 'support ticket retry was not idempotent');
  const conflictingSupport = await request('/support/diagnostics', {
    method: 'POST', token, body: { ...supportTicket, diagnostics: { ...supportTicket.diagnostics, errorCode: 'OTHER' } },
  });
  assert(conflictingSupport.status === 409, 'support ticket ID accepted changed diagnostics');
  const invalidSupportTimezone = await request('/support/diagnostics', {
    method: 'POST', token, body: { ...supportTicket, id: randomUUID(), diagnostics: { timezone: 'not-a-timezone' } },
  });
  assert(invalidSupportTimezone.status === 400, 'invalid support diagnostics timezone was accepted');

  const secondRegistration = await request('/auth/anonymous', {
    method: 'POST',
    body: {
      deviceId: 'awero-ci-device-two-0002',
      installationSecret: randomBytes(32).toString('base64url'),
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
  const crossOwnerEvents = await request(`/wake-sessions/${sessionId}/events`, { token: secondRegistration.data.accessToken });
  assert(crossOwnerEvents.status === 404, 'a different anonymous owner accessed wake session events');
  const otherOwnerStatistics = await request('/statistics/summary', { token: secondRegistration.data.accessToken });
  assert(
    otherOwnerStatistics.status === 200 && otherOwnerStatistics.data.summary.totalWakes === 0,
    'statistics leaked wake data across anonymous accounts',
  );
  const crossOwnerSync = await request('/sync', {
    method: 'POST', token: secondRegistration.data.accessToken, body: { operations: [syncOperation] },
  });
  assert(
    crossOwnerSync.status === 201 && crossOwnerSync.data.rejected[0].code === 'SYNC_OPERATION_ID_CONFLICT',
    'sync operation ID ownership conflict was not isolated safely',
  );
  const crossOwnerAnalytics = await request('/analytics/events', {
    method: 'POST', token: secondRegistration.data.accessToken, body: { events: [analyticsEvent] },
  });
  assert(
    crossOwnerAnalytics.status === 201 && crossOwnerAnalytics.data.rejected[0].code === 'ANALYTICS_EVENT_ID_CONFLICT',
    'analytics event ID ownership conflict was not isolated safely',
  );
  const crossOwnerSupport = await request('/support/diagnostics', {
    method: 'POST', token: secondRegistration.data.accessToken, body: supportTicket,
  });
  assert(crossOwnerSupport.status === 409, 'support ticket ID was reused across anonymous owners');

  for (let attempt = 0; attempt < 6; attempt += 1) {
    const repeatRegistration = await request('/auth/anonymous', {
      method: 'POST', body: {
        deviceId: 'awero-ci-device-one-0001', installationSecret, platform: 'IOS',
        appVersion: '0.1.0', timezone: 'Europe/Moscow',
      },
    });
    assert(
      repeatRegistration.status === 201 && repeatRegistration.data.anonymousUserId === registration.data.anonymousUserId,
      'repeat registration created another account or failed before the rate limit',
    );
  }
  const limitedRegistration = await request('/auth/anonymous', {
    method: 'POST', body: {
      deviceId: 'awero-ci-device-one-0001', installationSecret, platform: 'IOS',
      appVersion: '0.1.0', timezone: 'Europe/Moscow',
    },
  });
  assert(limitedRegistration.status === 429, 'anonymous registration rate limit was not enforced');

  const removed = await request(`/alarms/${alarmId}`, { method: 'DELETE', token });
  assert(removed.status === 200 && removed.data.version === 4, 'alarm delete/tombstone failed');
  const list = await request('/alarms', { token });
  assert(list.status === 200 && list.data.items.length === 0, 'deleted alarm remained in the list');

  process.stdout.write('AWERO API anonymous auth + alarm CRUD: PASS\n');
}

main().catch((error) => {
  process.stderr.write(`${error.message}\n`);
  process.exitCode = 1;
});
