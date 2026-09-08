// A small application fixture; all test actions and assertions are Java.
const toggle = document.getElementById('toggle');
toggle.addEventListener('click', () => {
  toggle.setAttribute('aria-pressed', toggle.getAttribute('aria-pressed') === 'true' ? 'false' : 'true');
});
document.getElementById('selection').addEventListener('change', event => {
  document.getElementById('status').textContent = event instanceof Event ? 'Selected ' + event.target.value : 'Wrong event realm';
});
document.getElementById('field').addEventListener('input', event => {
  document.getElementById('status').textContent = event instanceof InputEvent ? 'Typed ' + event.target.value : 'Wrong event realm';
});
setTimeout(() => { document.body.setAttribute('data-ready', 'true'); }, 30);
