import { test, expect } from '@playwright/test';

test.describe('Payment API Flow', () => {
  const baseURL = process.env.BASE_URL || 'http://localhost:8084';

  test('should process bank transfer payment with idempotency', async ({ request }) => {
    const idempotencyKey = 'test-bank-payment-' + Date.now();

    const response = await request.post(`${baseURL}/rest/api/payments/pay/bank-transfer`, {
      headers: {
        'Idempotency-Key': idempotencyKey,
        'Content-Type': 'application/json'
      },
      data: {
        amount: '1000.00',
        receiverId: 'RECEIVER123',
        accountNumber: '1234567890'
      }
    });

    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    expect(body.response_code).toBeDefined();

    // Try duplicate request
    const duplicateResponse = await request.post(`${baseURL}/rest/api/payments/pay/bank-transfer`, {
      headers: {
        'Idempotency-Key': idempotencyKey,
        'Content-Type': 'application/json'
      },
      data: {
        amount: '1000.00',
        receiverId: 'RECEIVER123',
        accountNumber: '1234567890'
      }
    });

    expect(duplicateResponse.status()).toBe(400);
    const duplicateBody = await duplicateResponse.json();
    expect(duplicateBody.response_description).toContain('already processed');
  });

  test('should reject payment without idempotency key', async ({ request }) => {
    const response = await request.post(`${baseURL}/rest/api/payments/pay/bank-transfer`, {
      headers: {
        'Content-Type': 'application/json'
      },
      data: {
        amount: '1000.00',
        receiverId: 'RECEIVER123'
      }
    });

    expect(response.status()).toBe(400);
    const body = await response.json();
    expect(body.response_description).toContain('Idempotency-Key header is required');
  });

  test('should process MoMo payment to merchant', async ({ request }) => {
    const response = await request.post(`${baseURL}/rest/api/payments/pay/momo/to-merchant`, {
      headers: {
        'Content-Type': 'application/json'
      },
      data: {
        amount: '500.00',
        receiverId: 'RECEIVER123',
        senderId: 'SENDER123'
      }
    });

    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    expect(body.response).toBeDefined();
  });

  test('should process card payment', async ({ request }) => {
    const response = await request.post(`${baseURL}/rest/api/payments/pay/card`, {
      headers: {
        'Content-Type': 'application/json'
      },
      data: {
        amount: '2000.00',
        currency: 'ZMW',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        phone: '+260971234567'
      }
    });

    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    expect(body.status).toBeDefined();
  });

  test('should handle webhook callback', async ({ request }) => {
    const referenceNo = 'TEST_REF_' + Date.now();
    
    const response = await request.post(`${baseURL}/rest/api/payments/callback/zynlepay`, {
      headers: {
        'Content-Type': 'application/json'
      },
      data: {
        responseCode: '100',
        referenceNo: referenceNo,
        amount: '1000.00',
        operatorReference: 'OP123'
      }
    });

    expect(response.ok()).toBeTruthy();

    // Try duplicate webhook
    const duplicateResponse = await request.post(`${baseURL}/rest/api/payments/callback/zynlepay`, {
      headers: {
        'Content-Type': 'application/json'
      },
      data: {
        responseCode: '100',
        referenceNo: referenceNo,
        amount: '1000.00',
        operatorReference: 'OP123'
      }
    });

    // Should be idempotent - may return success or conflict
    expect([200, 409]).toContain(duplicateResponse.status());
  });
});
