// Shader menu for the player controls (Windows libmpv).
// State arrives as `shaders` in the controls JSON (see DesktopLibmpvShaders.kt);
// choices go back as `shadersToggle` / `shadersSelect` events.
(() => {
  const actions = document.querySelector(".header-actions");
  if (!actions || typeof window.playerControls !== "function") return;

  const sendShaderEvent = (type, value) => {
    const bridge = window.chrome && window.chrome.webview;
    if (bridge) bridge.postMessage({ type, value });
  };

  let shaderState = null;
  let menuOpen = false;

  const svgNs = "http://www.w3.org/2000/svg";
  const tvIcon = () => {
    const svg = document.createElementNS(svgNs, "svg");
    svg.setAttribute("viewBox", "0 0 24 24");
    const path = document.createElementNS(svgNs, "path");
    path.setAttribute("d", "M21 3H3a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h5v2h8v-2h5a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2Zm0 14H3V5h18v12ZM5 7h14v1.2H5V7Zm0 2.6h14v1.2H5V9.6Zm0 2.6h14v1.2H5v-1.2Z");
    svg.appendChild(path);
    return svg;
  };

  const root = document.createElement("div");
  root.className = "shaders";
  root.hidden = true;

  const button = document.createElement("button");
  button.type = "button";
  button.className = "header-button shaders-button";
  button.setAttribute("aria-label", "Shaders");
  button.setAttribute("aria-haspopup", "true");
  button.setAttribute("aria-expanded", "false");
  button.appendChild(tvIcon());

  const menu = document.createElement("div");
  menu.className = "shaders-menu";
  menu.setAttribute("role", "menu");
  menu.hidden = true;

  root.append(button, menu);
  const anchor = document.getElementById("videoSettingsButton");
  actions.insertBefore(root, anchor || actions.firstChild);

  const element = (tag, className, text) => {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text != null) node.textContent = text;
    return node;
  };

  const renderMenu = () => {
    menu.replaceChildren();
    if (!shaderState) return;
    const presets = Array.isArray(shaderState.presets) ? shaderState.presets : [];
    const active = Boolean(shaderState.active);

    const header = element("div", "shaders-header");
    header.append(element("span", "shaders-title", "Shaders"));
    const toggle = element("button", "shaders-switch" + (active ? " on" : ""));
    toggle.type = "button";
    toggle.setAttribute("role", "switch");
    toggle.setAttribute("aria-checked", String(active));
    toggle.setAttribute("aria-label", active ? "Turn shader off" : "Turn shader on");
    toggle.append(element("span", "shaders-knob"));
    toggle.addEventListener("click", event => {
      event.stopPropagation();
      shaderState.active = !active;
      sendShaderEvent("shadersToggle", shaderState.active ? 1 : 0);
      render();
    });
    header.append(toggle);
    menu.append(header);

    const list = element("div", "shaders-list");
    presets.forEach((preset, index) => {
      const selected = index === shaderState.selected;
      const problem = typeof preset.problem === "string" ? preset.problem : "";
      const item = element("button", "shaders-item" + (selected ? " selected" : "") + (selected && !active ? " dimmed" : "") + (problem ? " unsupported" : ""));
      item.type = "button";
      item.setAttribute("role", "menuitemradio");
      item.setAttribute("aria-checked", String(selected));
      item.append(element("span", "shaders-check", selected ? "✓" : ""));
      const label = element("span", "shaders-name");
      label.append(element("span", "", preset.name || "Shader"));
      if (problem) label.append(element("span", "shaders-problem", problem));
      item.append(label);
      if (preset.user) item.append(element("span", "shaders-tag", "Custom"));
      if (problem) {
        item.disabled = true;
        item.title = problem;
        list.append(item);
        return;
      }
      item.addEventListener("click", event => {
        event.stopPropagation();
        shaderState.selected = index;
        shaderState.active = true;
        sendShaderEvent("shadersSelect", index);
        render();
      });
      list.append(item);
    });
    menu.append(list);

    if (shaderState.userFolder) {
      const hint = element("div", "shaders-hint");
      hint.append(element("span", "", "Add your own .glsl shaders to"));
      hint.append(element("code", "", shaderState.userFolder));
      hint.append(element("span", "", "They appear here on the next video."));
      if (shaderState.logFile) {
        hint.append(element("span", "", "Shader not working? Errors are logged to"));
        hint.append(element("code", "", shaderState.logFile));
      }
      menu.append(hint);
    }
  };

  const render = () => {
    root.hidden = !shaderState;
    button.classList.toggle("is-active", Boolean(shaderState && shaderState.active));
    button.setAttribute("aria-expanded", String(menuOpen));
    menu.hidden = !menuOpen || !shaderState;
    if (menuOpen) renderMenu();
  };

  const setMenuOpen = open => {
    menuOpen = open;
    render();
  };

  button.addEventListener("click", event => {
    event.stopPropagation();
    setMenuOpen(!menuOpen);
  });
  menu.addEventListener("click", event => event.stopPropagation());
  document.addEventListener("pointerdown", event => {
    if (menuOpen && !root.contains(event.target)) setMenuOpen(false);
  }, true);
  document.addEventListener("keydown", event => {
    if (menuOpen && event.key === "Escape") {
      event.stopPropagation();
      setMenuOpen(false);
    }
  }, true);

  const upstreamPlayerControls = window.playerControls;
  window.playerControls = nextState => {
    upstreamPlayerControls(nextState);
    const next = nextState && typeof nextState === "object" ? nextState.shaders : null;
    shaderState = next && typeof next === "object" ? { ...next } : null;
    if (!shaderState) menuOpen = false;
    render();
  };
})();
