export default function HeaderBar() {
  // No authentication required - simplified header with just logo and title
  return (
    <div className="headerbar-parent">
      {/* Logo & Title Container */}
      <div className="headerbar-logo-container">
        <img src="/assets/secureu-log-w.png" alt="Logo" className="headerbar-logo" />
        <span className="headerbar-title">NEXT GEN SIEM</span>
      </div>
    </div>
  );
}
