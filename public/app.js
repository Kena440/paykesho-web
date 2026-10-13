const overlay = document.querySelector("#auth-overlay");
const title = document.querySelector("#auth-title");
const kicker = document.querySelector("#auth-kicker");
const intro = document.querySelector("#auth-intro");
const message = document.querySelector("#auth-message");
const signupForm = document.querySelector("#signup-form");
const signinForm = document.querySelector("#signin-form");
const resetForm = document.querySelector("#reset-form");
const signedInPanel = document.querySelector("#signed-in-panel");
const signedInCopy = document.querySelector("#signed-in-copy");
const signoutButton = document.querySelector("#signout-button");
const authTabs = [...document.querySelectorAll(".auth-tabs [data-view]")];
let currentView = "signup";
let busy = false;

document.querySelector("#year").textContent = String(new Date().getFullYear());

function setMessage(text, isError = false) {
  message.textContent = text;
  message.classList.toggle("error", isError);
  message.hidden = !text;
}
function setBusy(form, value) {
  busy = value;
  const button = form.querySelector('button[type="submit"]');
  if (button) {
    if (!button.dataset.originalText) button.dataset.originalText = button.textContent;
    button.disabled = value;
    button.textContent = value ? "Please wait…" : button.dataset.originalText;
  }
}
function showView(view) {
  currentView = view;
  overlay.hidden = false;
  document.body.style.overflow = "hidden";
  setMessage("");
  signedInPanel.hidden = true;
  signupForm.hidden = view !== "signup";
  signinForm.hidden = view !== "signin";
  resetForm.hidden = view !== "reset";
  const config = {
    signup: ["Create your account", "WELCOME TO PAYKESHO", "Start with your details. We'll help you take it from there."],
    signin: ["Welcome back", "GOOD TO SEE YOU", "Sign in securely to your PayKesho account."],
    reset: ["Reset your password", "ACCOUNT RECOVERY", "Enter your email and we'll send reset instructions."]
  }[view];
  title.textContent = config[0];
  kicker.textContent = config[1];
  intro.textContent = config[2];
  authTabs.forEach((tab) => {
    tab.setAttribute("aria-selected", String(tab.dataset.view === view));
  });
  const focusTarget = ({ signup: "#full-name", signin: "#signin-email", reset: "#reset-email" })[view];
  window.setTimeout(() => document.querySelector(focusTarget)?.focus(), 0);
}
function closeView() {
  overlay.hidden = true;
  document.body.style.overflow = "";
}
async function api(path, payload, token) {
  const response = await fetch(path, {
    method: path === "/api/auth/user" ? "GET" : "POST",
    headers: {
      ...(payload ? { "content-type": "application/json" } : {}),
      ...(token ? { authorization: `Bearer ${token}` } : {})
    },
    ...(payload ? { body: JSON.stringify(payload) } : {})
  });
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    const description = data && (data.msg || data.message || data.error_description || data.error);
    throw new Error(typeof description === "string" ? description : "Something went wrong. Please try again.");
  }
  return data;
}
function safeMessage(error) {
  const value = error instanceof Error ? error.message : "";
  if (/invalid login credentials/i.test(value)) return "The email or password is incorrect.";
  if (/already registered|already exists/i.test(value)) return "An account with these details may already exist. Try signing in.";
  if (/password should be at least|weak password/i.test(value)) return "Choose a stronger password with at least 8 characters.";
  return value || "We couldn't complete that request. Please try again.";
}

document.querySelectorAll("[data-view]").forEach((button) => {
  button.addEventListener("click", () => showView(button.dataset.view));
});
document.querySelectorAll("[data-close]").forEach((button) => button.addEventListener("click", closeView));
overlay.addEventListener("click", (event) => { if (event.target === overlay) closeView(); });
document.addEventListener("keydown", (event) => { if (event.key === "Escape" && !overlay.hidden) closeView(); });

signupForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (busy) return;
  const form = new FormData(signupForm);
  const payload = {
    full_name: String(form.get("full_name") || "").trim(),
    phone_number: String(form.get("phone_number") || "").trim(),
    email: String(form.get("email") || "").trim(),
    password: String(form.get("password") || "")
  };
  setBusy(signupForm, true);
  setMessage("");
  try {
    const data = await api("/api/auth/signup", payload);
    if (data?.access_token) {
      sessionStorage.setItem("paykesho_access_token", data.access_token);
      await showSignedIn(data.user);
    } else {
      setMessage("Your registration request was received. Check your email for a confirmation link if email confirmation is enabled.");
      signupForm.reset();
    }
  } catch (error) {
    setMessage(safeMessage(error), true);
  } finally {
    setBusy(signupForm, false);
  }
});

signinForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (busy) return;
  const form = new FormData(signinForm);
  const payload = { email: String(form.get("email") || "").trim(), password: String(form.get("password") || "") };
  setBusy(signinForm, true);
  setMessage("");
  try {
    const data = await api("/api/auth/signin", payload);
    if (!data?.access_token) throw new Error("We couldn't start a session. Please try again.");
    sessionStorage.setItem("paykesho_access_token", data.access_token);
    await showSignedIn(data.user);
  } catch (error) {
    setMessage(safeMessage(error), true);
  } finally {
    setBusy(signinForm, false);
  }
});

resetForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (busy) return;
  const form = new FormData(resetForm);
  setBusy(resetForm, true);
  setMessage("");
  try {
    await api("/api/auth/password-reset", { email: String(form.get("email") || "").trim() });
    setMessage("If an account exists for that email, password reset instructions will be sent.");
  } catch (error) {
    setMessage(safeMessage(error), true);
  } finally {
    setBusy(resetForm, false);
  }
});

async function showSignedIn(user) {
  signupForm.hidden = true;
  signinForm.hidden = true;
  resetForm.hidden = true;
  document.querySelector(".auth-tabs").hidden = true;
  kicker.textContent = "SIGNED IN";
  title.textContent = "You're signed in";
  intro.textContent = "Your PayKesho account session is active in this tab.";
  const displayName = user?.user_metadata?.full_name || user?.email || "there";
  signedInCopy.textContent = `Welcome, ${displayName}. Profile linking and credit, voucher and payment workflows are still being completed, so those services aren't available in this preview yet.`;
  signedInPanel.hidden = false;
  setMessage("");
}
signoutButton.addEventListener("click", async () => {
  const token = sessionStorage.getItem("paykesho_access_token");
  try {
    if (token) await api("/api/auth/signout", {}, token);
  } catch (_) {
    // Always clear the local tab session even if the upstream sign-out fails.
  } finally {
    sessionStorage.removeItem("paykesho_access_token");
    document.querySelector(".auth-tabs").hidden = false;
    showView("signin");
  }
});

async function restoreSession() {
  const token = sessionStorage.getItem("paykesho_access_token");
  if (!token) return;
  try {
    const user = await api("/api/auth/user", null, token);
    showView("signin");
    await showSignedIn(user);
  } catch (_) {
    sessionStorage.removeItem("paykesho_access_token");
  }
}
restoreSession();
