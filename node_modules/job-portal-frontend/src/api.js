export const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/$/, '')

export async function api(path, { token, ...options } = {}) {
  let response
  try {
    response = await fetch(`${API_URL}${path}`, {
      ...options,
      headers: {
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    })
  } catch (exception) {
    if (exception instanceof TypeError) {
      throw new Error(
        `Cannot reach the API at ${API_URL}. Start the Spring Boot backend and check that the frontend and API use matching localhost addresses.`,
      )
    }
    throw exception
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || body.detail || body.error || `Request failed (${response.status})`)
  }
  if (response.status === 204) return null
  return response.json()
}
