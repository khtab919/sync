import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from './server.js';

test('health endpoint responds with JSON', async () => {
  const server = createServer();
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  try {
    const address = server.address();
    const response = await fetch('http://127.0.0.1:' + address.port + '/health');
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { status: 'ok', service: 'sync-backend' });
  } finally {
    await new Promise((resolve, reject) => server.close(err => err ? reject(err) : resolve()));
  }
});
