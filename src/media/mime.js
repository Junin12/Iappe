// MP4 primeiro (iPhone e Chrome recentes gravam H.264); WebM só como reserva.
const WITH_AUDIO = [
  'video/mp4;codecs=avc1.640028,mp4a.40.2',
  'video/mp4;codecs=avc1.42E01E,mp4a.40.2',
  'video/mp4;codecs=avc1,mp4a.40.2',
  'video/mp4',
  'video/webm;codecs=vp9,opus',
  'video/webm;codecs=vp8,opus',
  'video/webm',
];
const NO_AUDIO = ['video/mp4;codecs=avc1.640028', 'video/mp4;codecs=avc1.42E01E', 'video/mp4;codecs=avc1', 'video/mp4', 'video/webm;codecs=vp9', 'video/webm'];

export function pickMime(audio = true) {
  if (typeof MediaRecorder === 'undefined') return null;
  for (const m of audio ? WITH_AUDIO : NO_AUDIO) {
    if (MediaRecorder.isTypeSupported?.(m)) return m;
  }
  return '';
}

export const extFor = (mime) => (String(mime).includes('mp4') ? 'mp4' : 'webm');
export const baseType = (mime) => String(mime || 'video/mp4').split(';')[0];
