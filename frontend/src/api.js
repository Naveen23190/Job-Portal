
const API_URL = (
  import.meta.env.VITE_API_URL ||
  'https://jobhire-api-ul1c.onrender.com/api'
).replace(/\/+$/, '');

export { API_URL };

export async function api(path, { token, ...options } = {}) {
  let response;

  // Ensure paths work with or without a leading slash.
  const endpoint = `${API_URL}/${path.replace(/^\/+/, '')}`;

  try {
    response = await fetch(endpoint, {
      ...options,
      headers: {
        ...(options.body &&
        !(options.body instanceof FormData)
          ? { 'Content-Type': 'application/json' }
          : {}),
        ...(token
          ? { Authorization: `Bearer ${token}` }
          : {}),
        ...options.headers,
      },
    });
  } catch (exception) {
    if (exception instanceof TypeError) {
      throw new Error(
        `Unable to connect to JobHire API: ${API_URL}. Check backend availability and CORS configuration.`
      );
    }

    throw exception;
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));

    throw new Error(
      body.message ||
      body.detail ||
      body.error ||
      `Request failed (${response.status})`
    );
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}
