// Landing-only effects; vault timers and code generation are unaffected.
let cleanup = () => {};

export function stopLandingMotion() {
  cleanup();
  cleanup = () => {};
}

export function startLandingMotion(landing) {
  stopLandingMotion();
  if (!landing) return;
  const reduced = matchMedia('(prefers-reduced-motion: reduce)');
  const toggle = landing.querySelector('.motion-toggle');
  const targets = [...landing.querySelectorAll('.section-top, .feature-grid article, .recovery-layout > div, .recovery-steps li, .faq > div, .final-cta')];
  let paused = false;
  let observer;
  try { paused = sessionStorage.getItem('sentinel.motion-paused') === 'true'; } catch {}

  function applyPolicy() {
    observer?.disconnect();
    targets.forEach(el => el.classList.remove('motion-reveal', 'is-in-view'));
    landing.classList.toggle('motion-paused', paused);
    toggle.hidden = reduced.matches;
    toggle.setAttribute('aria-pressed', String(paused));
    toggle.setAttribute('aria-label', paused ? 'Resume animations' : 'Pause animations');
    toggle.title = paused ? 'Resume animations' : 'Pause animations';
    if (!reduced.matches && !paused && 'IntersectionObserver' in window) {
      observer = new IntersectionObserver(entries => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-in-view');
            observer.unobserve(entry.target);
          }
        });
      }, { threshold: 0.08 });
      targets.forEach(el => {
        // Content already on screen stays visible while off-screen sections reveal once.
        if (el.getBoundingClientRect().top >= innerHeight) {
          el.classList.add('motion-reveal');
          observer.observe(el);
        }
      });
    }
  }
  function visibility() { landing.classList.toggle('motion-suspended', document.hidden); }
  function onToggle() {
    paused = !paused;
    try { sessionStorage.setItem('sentinel.motion-paused', String(paused)); } catch {}
    applyPolicy();
  }
  toggle.addEventListener('click', onToggle);
  reduced.addEventListener('change', applyPolicy);
  document.addEventListener('visibilitychange', visibility);
  visibility();
  applyPolicy();
  cleanup = () => {
    observer?.disconnect();
    toggle.removeEventListener('click', onToggle);
    reduced.removeEventListener('change', applyPolicy);
    document.removeEventListener('visibilitychange', visibility);
  };
}
