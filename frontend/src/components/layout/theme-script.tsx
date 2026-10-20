/** Sets the saved theme before first paint, so there is no light flash. */
const SCRIPT = `try{var t=localStorage.getItem("bms.theme");if(t==="dark"||(!t&&matchMedia("(prefers-color-scheme: dark)").matches))document.documentElement.classList.add("dark")}catch(e){}`;

export function ThemeScript() {
  return <script dangerouslySetInnerHTML={{ __html: SCRIPT }} />;
}
