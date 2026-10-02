// Shared helper functions used by all three pages.

// Send a GET request and return {status, data}
async function apiGet(url) {
  const response = await fetch(url);
  const data = await response.json().catch(() => ({}));
  return { status: response.status, data: data };
}

// Send a POST request with form fields and return {status, data}
async function apiPost(url, fields) {
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams(fields)
  });
  const data = await response.json().catch(() => ({}));
  return { status: response.status, data: data };
}

// Create a <td> with plain text. textContent (not innerHTML) prevents XSS.
function cell(text) {
  const td = document.createElement("td");
  td.textContent = text === null || text === undefined ? "" : text;
  return td;
}

// Create a coloured status badge
function statusBadge(status) {
  const span = document.createElement("span");
  span.className = "badge " + status.replace(" ", "-");
  span.textContent = status;
  return span;
}

// Show a message under a form. type = "ok" or "error"
function showMessage(elementId, text, type) {
  const el = document.getElementById(elementId);
  el.textContent = text;
  el.className = "msg " + type;
}

async function logout() {
  await apiPost("api/logout", {});
  window.location.href = "login.html";
}
