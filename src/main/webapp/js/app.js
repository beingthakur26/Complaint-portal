// ResolveHub - Shared application helpers and interactive modals

let currentUser = null;

// Send a GET request and return {status, data}
async function apiGet(url) {
  try {
    const response = await fetch(url);
    const data = await response.json().catch(() => ({}));
    return { status: response.status, data: data };
  } catch (err) {
    console.error("Network error on GET " + url, err);
    return { status: 500, data: { success: false, message: "Network connection error." } };
  }
}

// Send a POST request with form fields and return {status, data}
async function apiPost(url, fields) {
  try {
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams(fields)
    });
    const data = await response.json().catch(() => ({}));
    return { status: response.status, data: data };
  } catch (err) {
    console.error("Network error on POST " + url, err);
    return { status: 500, data: { success: false, message: "Network connection error." } };
  }
}

// Format timestamp to user-friendly readable date string
function formatDate(dateStr) {
  if (!dateStr) return "-";
  try {
    const d = new Date(dateStr.replace(" ", "T"));
    if (isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
      hour12: true
    });
  } catch (e) {
    return dateStr;
  }
}

// Extract 1-2 letter initials from a full name
function getInitials(name) {
  if (!name) return "U";
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

// Create a <td> with plain text. textContent prevents XSS.
function cell(text, className) {
  const td = document.createElement("td");
  td.textContent = text === null || text === undefined ? "" : text;
  if (className) td.className = className;
  return td;
}

// Create a modern status badge with status dot indicator
function statusBadge(status) {
  const safeStatus = (status || "PENDING").toUpperCase().trim();
  const span = document.createElement("span");
  span.className = "badge " + safeStatus.replace(/\s+/g, "-");
  
  const dot = document.createElement("span");
  dot.className = "badge-dot";
  span.appendChild(dot);

  const textNode = document.createTextNode(safeStatus);
  span.appendChild(textNode);
  return span;
}

// Inline message banner under forms
function showMessage(elementId, text, type) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.textContent = text;
  el.className = "msg " + type;
}

// Modern floating toast notification
function showToast(text, type = "ok", duration = 3500) {
  let container = document.getElementById("toastContainer");
  if (!container) {
    container = document.createElement("div");
    container.id = "toastContainer";
    container.className = "toast-container";
    document.body.appendChild(container);
  }

  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;

  const iconSvg = type === "ok" 
    ? `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>`
    : `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>`;

  toast.innerHTML = `
    <div style="display:flex;align-items:center;">${iconSvg}</div>
    <div class="toast-message">${escapeHtml(text)}</div>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateY(10px) scale(0.95)";
    setTimeout(() => toast.remove(), 250);
  }, duration);
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str === null || str === undefined ? "" : str;
  return div.innerHTML;
}

// Global Logout
async function logout() {
  await apiPost("api/logout", {});
  showToast("Logged out successfully.", "ok", 1500);
  setTimeout(() => {
    window.location.href = "login.html";
  }, 300);
}

// ==========================================
// USER PROFILE MODAL & LOGIC
// ==========================================

function ensureProfileModalDom() {
  if (document.getElementById("profileModal")) return;

  const modalHtml = `
    <div id="profileModal" class="modal-backdrop" style="display:none;" onclick="if(event.target===this) closeProfileModal()">
      <div class="modal-card">
        <div class="modal-header">
          <div class="modal-title-group">
            <div class="modal-avatar" id="modalUserAvatar">U</div>
            <div>
              <h3 id="modalUserNameTitle">User Profile</h3>
              <p id="modalUserRoleBadge" class="modal-role-pill">Student</p>
            </div>
          </div>
          <button type="button" class="modal-close-btn" onclick="closeProfileModal()" title="Close">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </button>
        </div>

        <form id="profileForm" onsubmit="event.preventDefault(); saveUserProfile();" class="modal-body">
          <div class="form-group">
            <label for="profName">Display Name</label>
            <div class="input-wrapper">
              <span class="input-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
              </span>
              <input type="text" id="profName" required maxlength="100" placeholder="Your full name">
            </div>
          </div>

          <div class="form-group">
            <label for="profEmail">Email Address</label>
            <div class="input-wrapper">
              <span class="input-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline></svg>
              </span>
              <input type="email" id="profEmail" required maxlength="100" placeholder="name@college.com">
            </div>
          </div>

          <div class="form-group">
            <label for="profNewPassword">New Password <span style="font-weight:400; color:var(--text-muted);">(Leave blank to keep unchanged)</span></label>
            <div class="input-wrapper">
              <span class="input-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
              </span>
              <input type="password" id="profNewPassword" placeholder="Minimum 6 characters" minlength="6" autocomplete="new-password">
            </div>
          </div>

          <div id="profMsg" class="msg"></div>

          <div class="modal-footer">
            <button type="button" class="btn-logout" onclick="logout()" style="margin-right:auto;">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
              <span>Logout</span>
            </button>
            <button type="button" class="btn-outline" onclick="closeProfileModal()">Cancel</button>
            <button type="submit" id="btnSaveProfile">Save Changes</button>
          </div>
        </form>
      </div>
    </div>
  `;

  document.body.insertAdjacentHTML("beforeend", modalHtml);
}

function openProfileModal() {
  ensureProfileModalDom();
  if (currentUser) {
    document.getElementById("profName").value = currentUser.name || "";
    document.getElementById("profEmail").value = currentUser.email || "";
    document.getElementById("profNewPassword").value = "";
    document.getElementById("modalUserNameTitle").textContent = currentUser.name || "User Profile";
    document.getElementById("modalUserAvatar").textContent = getInitials(currentUser.name);
    
    const roleBadge = document.getElementById("modalUserRoleBadge");
    if (roleBadge) {
      roleBadge.textContent = currentUser.role === "ADMIN" ? "Administrator" : "Student";
      roleBadge.className = currentUser.role === "ADMIN" ? "modal-role-pill admin" : "modal-role-pill student";
    }
  }
  const modal = document.getElementById("profileModal");
  modal.style.display = "flex";
  document.getElementById("profMsg").style.display = "none";
}

function closeProfileModal() {
  const modal = document.getElementById("profileModal");
  if (modal) modal.style.display = "none";
}

async function saveUserProfile() {
  const name = document.getElementById("profName").value.trim();
  const email = document.getElementById("profEmail").value.trim();
  const newPassword = document.getElementById("profNewPassword").value;

  if (!name || !email) {
    showMessage("profMsg", "Name and email are required.", "error");
    return;
  }
  if (newPassword && newPassword.length < 6) {
    showMessage("profMsg", "New password must be at least 6 characters.", "error");
    return;
  }

  const btn = document.getElementById("btnSaveProfile");
  btn.disabled = true;
  btn.textContent = "Saving...";

  const res = await apiPost("api/profile", { name, email, newPassword });
  btn.disabled = false;
  btn.textContent = "Save Changes";

  if (res.data && res.data.success) {
    showToast("Profile updated successfully!", "ok");
    showMessage("profMsg", "Profile updated successfully.", "ok");
    currentUser.name = name;
    currentUser.email = email;

    // Update header badges
    const userBadgeName = document.getElementById("userName");
    if (userBadgeName) userBadgeName.textContent = name;
    const adminBadgeName = document.getElementById("adminName");
    if (adminBadgeName) adminBadgeName.textContent = name;

    const userAvatar = document.getElementById("userAvatar");
    if (userAvatar) userAvatar.textContent = getInitials(name);
    const adminAvatar = document.getElementById("adminAvatar");
    if (adminAvatar) adminAvatar.textContent = getInitials(name);

    setTimeout(() => {
      closeProfileModal();
    }, 1000);
  } else {
    const errorMsg = (res.data && res.data.message) || "Failed to update profile.";
    showMessage("profMsg", errorMsg, "error");
    showToast(errorMsg, "error");
  }
}

// ==========================================
// GRIEVANCE DETAIL MODAL
// ==========================================

function ensureDetailModalDom() {
  if (document.getElementById("grievanceDetailModal")) return;

  const modalHtml = `
    <div id="grievanceDetailModal" class="modal-backdrop" style="display:none;" onclick="if(event.target===this) closeGrievanceModal()">
      <div class="modal-card modal-card-lg">
        <div class="modal-header">
          <div>
            <div style="display:flex; align-items:center; gap:10px; margin-bottom:4px;">
              <span class="id-tag" id="detailTicketId">#0000</span>
              <span id="detailStatusBadge"></span>
            </div>
            <h3 id="detailTitle" style="font-size:1.15rem; color:var(--text-primary); font-weight:700;">Grievance Title</h3>
          </div>
          <button type="button" class="modal-close-btn" onclick="closeGrievanceModal()" title="Close">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </button>
        </div>

        <div class="modal-body">
          <div class="detail-meta-grid">
            <div class="detail-meta-box">
              <span class="meta-label">Submitted By</span>
              <span class="meta-val" id="detailUser">Student</span>
            </div>
            <div class="detail-meta-box">
              <span class="meta-label">Filed On</span>
              <span class="meta-val" id="detailDate">-</span>
            </div>
            <div class="detail-meta-box">
              <span class="meta-label">Department</span>
              <span class="meta-val" id="detailDept">General</span>
            </div>
          </div>

          <div class="detail-desc-box">
            <h4>Description</h4>
            <p id="detailDesc"></p>
          </div>

          <div id="adminActionSection" style="display:none; margin-top:20px; padding-top:16px; border-top:1px solid var(--border-subtle);">
            <h4>Update Resolution Status</h4>
            <div style="display:flex; gap:10px; margin-top:10px; align-items:center;">
              <select id="detailStatusSelect" class="status-select" style="min-width:160px;">
                <option value="PENDING">PENDING</option>
                <option value="IN PROGRESS">IN PROGRESS</option>
                <option value="RESOLVED">RESOLVED</option>
              </select>
              <button type="button" id="btnUpdateDetailStatus" onclick="applyDetailStatusChange()">Update Status</button>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button type="button" class="btn-outline" onclick="closeGrievanceModal()">Close</button>
        </div>
      </div>
    </div>
  `;

  document.body.insertAdjacentHTML("beforeend", modalHtml);
}

let activeDetailTicketId = null;
let activeDetailCallback = null;

function openGrievanceModal(c, isAdmin = false, onStatusChange = null) {
  ensureDetailModalDom();
  activeDetailTicketId = c.id;
  activeDetailCallback = onStatusChange;

  document.getElementById("detailTicketId").textContent = "#" + c.id;
  document.getElementById("detailTitle").textContent = c.title || "No Title";
  document.getElementById("detailDesc").textContent = c.description || "No description provided.";
  document.getElementById("detailUser").textContent = c.userName || (currentUser ? currentUser.name : "Student");
  document.getElementById("detailDate").textContent = formatDate(c.createdAt);

  // Category detection from title prefix e.g. [Hostel]
  let dept = "General";
  const match = (c.title || "").match(/^\[(.*?)\]/);
  if (match) dept = match[1];
  document.getElementById("detailDept").textContent = dept;

  const badgeSlot = document.getElementById("detailStatusBadge");
  badgeSlot.innerHTML = "";
  badgeSlot.appendChild(statusBadge(c.status));

  const adminSec = document.getElementById("adminActionSection");
  if (isAdmin) {
    adminSec.style.display = "block";
    const select = document.getElementById("detailStatusSelect");
    select.value = (c.status || "PENDING").toUpperCase();
  } else {
    adminSec.style.display = "none";
  }

  document.getElementById("grievanceDetailModal").style.display = "flex";
}

function closeGrievanceModal() {
  const modal = document.getElementById("grievanceDetailModal");
  if (modal) modal.style.display = "none";
}

async function applyDetailStatusChange() {
  if (!activeDetailTicketId) return;
  const newStatus = document.getElementById("detailStatusSelect").value;
  if (typeof activeDetailCallback === "function") {
    await activeDetailCallback(activeDetailTicketId, newStatus);
    const badgeSlot = document.getElementById("detailStatusBadge");
    badgeSlot.innerHTML = "";
    badgeSlot.appendChild(statusBadge(newStatus));
  }
}
