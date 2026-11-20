'use strict';
// Server-side strings for the Responder Brief, SMS and journey page. Languages: en, hi, kn.
const L = {
  en: {
    unknown: 'Unknown', minutesAgo: (m) => (m < 1 ? 'just now' : `${m} min ago`),
    type: { fall: 'Fall', crash: 'Crash', sos: 'SOS', journey_alert: 'Journey alert' },
    status: { not_responding: 'User not responding', responsive: 'User responsive', cancelled: 'Cancelled by user', responder_on_way: 'Responder on the way' },
    sev: { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High' },
    patient: 'Patient', name: 'Name', age: 'Age', gender: 'Gender', blood: 'Blood group', allergies: 'Allergies',
    conditions: 'Conditions', medications: 'Medications', contacts: 'Emergency contacts',
    analysis: 'Event analysis', severity: 'Severity', peakG: 'Peak impact', freeFall: 'Free-fall', orient: 'Orientation change',
    stillness: 'Stillness after impact', location: 'Location', address: 'Address', coords: 'Coordinates', accuracy: 'Accuracy',
    lastUpdate: 'Last update', directions: 'Directions', nearest: 'Nearest help', call: 'Call', hospitals: 'Hospitals',
    clinics: 'Clinics', pharmacies: 'Pharmacies', guidance: 'Bystander guidance', aiLabel: 'AI-generated summary, verify on scene',
    seen: "I've seen this / on my way", acked: 'Acknowledged — responder on the way', seconds: 's', degrees: '°',
    interp: (e) => {
      const bits = [];
      const s = e.severityLevel ? ({ LOW: 'low-impact', MEDIUM: 'medium-impact', HIGH: 'high-impact' })[e.severityLevel] : null;
      const what = e.kind === 'crash' ? 'crash' : 'fall';
      if (s) bits.push(`${s} ${what}`); else bits.push(`${what} reported`);
      if (e.stillnessSec != null) bits.push(e.stillnessSec > 0 ? `followed by ${Math.round(e.stillnessSec)} s without movement` : 'followed by movement');
      return bits.join(' ');
    },
    summaryFallback: (d) => `${d.type} at ${d.time}. ${d.severity ? 'Severity ' + d.severity + '. ' : ''}${d.status}. Last known location: ${d.address}. Nearest hospital: ${d.hospital}.`,
    guide: {
      common: ['Call 112 / 108 now if you have not already.'],
      fall: ['Do not move the person if a neck, back or head injury is possible.', 'Check if they are awake and breathing.', 'Control any bleeding with firm pressure using a clean cloth.', 'Keep them warm and still until help arrives.'],
      crash: ['Switch off the vehicle engine; keep clear of traffic and fuel.', 'Do not remove a helmet or move the person unless there is immediate danger.', 'Check breathing; start CPR if not breathing and you know how.', 'Press firmly on any heavy bleeding.'],
      sos: ['Stay safe; do not confront anyone.', 'Note the location, people and vehicles around.', 'Keep the phone line open and share location with police.', 'Move to a safe, public place if you can.'],
      journey_alert: ['Try calling the person; do not assume they are safe.', 'Share the last known location with police if unreachable.', 'Note the vehicle and driver details shown here.', 'Check the route for where the phone last moved.'],
    },
    journey: { title: 'Safe Journey', to: 'To', eta: 'ETA', status: { on_route: 'On route', deviated: 'Deviated', stopped: 'Stopped', delayed: 'Delayed', arrived: 'Arrived', alert: 'Alert' }, callName: (n) => `Call ${n}`, cab: 'Cab details', vehicle: 'Vehicle', driver: 'Driver', battery: 'Battery', offline: 'Phone offline' },
    sms: {
      journeyStart: (n, place, eta, link) => `${n} started a journey to ${place}. ETA ${eta}. Track: ${link}`,
      arrived: (n) => `${n} arrived safely.`,
      watchdog: (n, mins, link, pos) => `ALERT: No update from ${n}'s phone for ${mins} min. Last known position: ${pos}. Track: ${link}`,
      overdue: (n, link, pos) => `ALERT: ${n} has not arrived and is overdue. Last known position: ${pos}. Track: ${link}`,
    },
  },
  hi: {
    unknown: 'अज्ञात', minutesAgo: (m) => (m < 1 ? 'अभी' : `${m} मिनट पहले`),
    type: { fall: 'गिरना', crash: 'दुर्घटना', sos: 'SOS', journey_alert: 'यात्रा अलर्ट' },
    status: { not_responding: 'उपयोगकर्ता जवाब नहीं दे रहा', responsive: 'उपयोगकर्ता जवाब दे रहा है', cancelled: 'उपयोगकर्ता ने रद्द किया', responder_on_way: 'सहायता रास्ते में है' },
    sev: { LOW: 'कम', MEDIUM: 'मध्यम', HIGH: 'उच्च' },
    patient: 'मरीज़', name: 'नाम', age: 'आयु', gender: 'लिंग', blood: 'रक्त समूह', allergies: 'एलर्जी',
    conditions: 'स्वास्थ्य स्थितियाँ', medications: 'दवाइयाँ', contacts: 'आपातकालीन संपर्क',
    analysis: 'घटना विश्लेषण', severity: 'गंभीरता', peakG: 'अधिकतम झटका', freeFall: 'फ्री-फॉल', orient: 'दिशा में बदलाव',
    stillness: 'झटके के बाद स्थिरता', location: 'स्थान', address: 'पता', coords: 'निर्देशांक', accuracy: 'सटीकता',
    lastUpdate: 'आखिरी अपडेट', directions: 'दिशा-निर्देश', nearest: 'नज़दीकी मदद', call: 'कॉल करें', hospitals: 'अस्पताल',
    clinics: 'क्लिनिक', pharmacies: 'दवा की दुकानें', guidance: 'आसपास के लोगों के लिए सलाह', aiLabel: 'AI द्वारा बनाया गया सारांश, मौके पर जाँच करें',
    seen: 'मैंने देखा / मैं आ रहा हूँ', acked: 'पुष्टि हुई — सहायता रास्ते में है', seconds: 'से', degrees: '°',
    interp: (e) => {
      const s = e.severityLevel ? ({ LOW: 'हल्का', MEDIUM: 'मध्यम', HIGH: 'तेज़' })[e.severityLevel] : null;
      const what = e.kind === 'crash' ? 'दुर्घटना' : 'गिरना';
      const a = s ? `${s} झटके वाली ${what}` : `${what} की सूचना`;
      return e.stillnessSec != null ? `${a}, उसके बाद ${Math.round(e.stillnessSec)} सेकंड कोई हलचल नहीं` : a;
    },
    summaryFallback: (d) => `${d.type}, समय ${d.time}. ${d.severity ? 'गंभीरता ' + d.severity + '. ' : ''}${d.status}. आखिरी स्थान: ${d.address}. नज़दीकी अस्पताल: ${d.hospital}.`,
    guide: {
      common: ['अभी 112 / 108 पर कॉल करें।'],
      fall: ['गर्दन, पीठ या सिर की चोट की आशंका हो तो व्यक्ति को न हिलाएँ।', 'देखें कि वे होश में हैं और साँस ले रहे हैं।', 'साफ कपड़े से कसकर दबाकर खून रोकें।', 'मदद आने तक उन्हें गर्म और स्थिर रखें।'],
      crash: ['वाहन का इंजन बंद करें; ट्रैफिक और ईंधन से दूर रहें।', 'ख़तरा न हो तो हेलमेट न हटाएँ और व्यक्ति को न हिलाएँ।', 'साँस जाँचें; साँस न हो और CPR आता हो तो शुरू करें।', 'ज़्यादा खून बहने पर कसकर दबाएँ।'],
      sos: ['सुरक्षित रहें; किसी का सामना न करें।', 'स्थान, लोगों और वाहनों को नोट करें।', 'फ़ोन लाइन चालू रखें और पुलिस को लोकेशन दें।', 'हो सके तो किसी सुरक्षित, भीड़ वाली जगह जाएँ।'],
      journey_alert: ['व्यक्ति को कॉल करने की कोशिश करें; उन्हें सुरक्षित न मानें।', 'संपर्क न हो तो आखिरी स्थान पुलिस को दें।', 'यहाँ दिखाए वाहन और ड्राइवर का विवरण नोट करें।', 'रूट देखें कि फ़ोन आखिरी बार कहाँ चला।'],
    },
    journey: { title: 'सुरक्षित यात्रा', to: 'गंतव्य', eta: 'पहुँचने का समय', status: { on_route: 'रास्ते पर', deviated: 'रास्ता बदला', stopped: 'रुके हुए', delayed: 'देरी', arrived: 'पहुँच गए', alert: 'अलर्ट' }, callName: (n) => `${n} को कॉल करें`, cab: 'कैब विवरण', vehicle: 'वाहन', driver: 'ड्राइवर', battery: 'बैटरी', offline: 'फ़ोन ऑफ़लाइन' },
    sms: {
      journeyStart: (n, place, eta, link) => `${n} ने ${place} की यात्रा शुरू की। पहुँचने का समय ${eta}। ट्रैक करें: ${link}`,
      arrived: (n) => `${n} सुरक्षित पहुँच गए।`,
      watchdog: (n, mins, link, pos) => `अलर्ट: ${n} के फ़ोन से ${mins} मिनट से कोई अपडेट नहीं। आखिरी स्थान: ${pos}। ट्रैक करें: ${link}`,
      overdue: (n, link, pos) => `अलर्ट: ${n} अभी तक नहीं पहुँचे। आखिरी स्थान: ${pos}। ट्रैक करें: ${link}`,
    },
  },
  kn: {
    unknown: 'ಗೊತ್ತಿಲ್ಲ', minutesAgo: (m) => (m < 1 ? 'ಈಗಷ್ಟೇ' : `${m} ನಿಮಿಷಗಳ ಹಿಂದೆ`),
    type: { fall: 'ಬೀಳುವಿಕೆ', crash: 'ಅಪಘಾತ', sos: 'SOS', journey_alert: 'ಪ್ರಯಾಣ ಎಚ್ಚರಿಕೆ' },
    status: { not_responding: 'ಬಳಕೆದಾರರು ಪ್ರತಿಕ್ರಿಯಿಸುತ್ತಿಲ್ಲ', responsive: 'ಬಳಕೆದಾರರು ಪ್ರತಿಕ್ರಿಯಿಸುತ್ತಿದ್ದಾರೆ', cancelled: 'ಬಳಕೆದಾರರು ರದ್ದುಗೊಳಿಸಿದ್ದಾರೆ', responder_on_way: 'ಸಹಾಯಕರು ಬರುತ್ತಿದ್ದಾರೆ' },
    sev: { LOW: 'ಕಡಿಮೆ', MEDIUM: 'ಮಧ್ಯಮ', HIGH: 'ಹೆಚ್ಚು' },
    patient: 'ರೋಗಿ', name: 'ಹೆಸರು', age: 'ವಯಸ್ಸು', gender: 'ಲಿಂಗ', blood: 'ರಕ್ತದ ಗುಂಪು', allergies: 'ಅಲರ್ಜಿಗಳು',
    conditions: 'ಆರೋಗ್ಯ ಸ್ಥಿತಿಗಳು', medications: 'ಔಷಧಗಳು', contacts: 'ತುರ್ತು ಸಂಪರ್ಕಗಳು',
    analysis: 'ಘಟನೆಯ ವಿಶ್ಲೇಷಣೆ', severity: 'ತೀವ್ರತೆ', peakG: 'ಗರಿಷ್ಠ ಆಘಾತ', freeFall: 'ಫ್ರೀ-ಫಾಲ್', orient: 'ದಿಕ್ಕಿನ ಬದಲಾವಣೆ',
    stillness: 'ಆಘಾತದ ನಂತರ ನಿಶ್ಚಲತೆ', location: 'ಸ್ಥಳ', address: 'ವಿಳಾಸ', coords: 'ನಿರ್ದೇಶಾಂಕಗಳು', accuracy: 'ನಿಖರತೆ',
    lastUpdate: 'ಕೊನೆಯ ಅಪ್‌ಡೇಟ್', directions: 'ದಾರಿ', nearest: 'ಹತ್ತಿರದ ಸಹಾಯ', call: 'ಕರೆ ಮಾಡಿ', hospitals: 'ಆಸ್ಪತ್ರೆಗಳು',
    clinics: 'ಕ್ಲಿನಿಕ್‌ಗಳು', pharmacies: 'ಔಷಧ ಅಂಗಡಿಗಳು', guidance: 'ಸುತ್ತಲಿನವರಿಗೆ ಸಲಹೆ', aiLabel: 'AI ರಚಿಸಿದ ಸಾರಾಂಶ, ಸ್ಥಳದಲ್ಲಿ ಪರಿಶೀಲಿಸಿ',
    seen: 'ನೋಡಿದೆ / ಬರುತ್ತಿದ್ದೇನೆ', acked: 'ದೃಢೀಕರಿಸಲಾಗಿದೆ — ಸಹಾಯಕರು ಬರುತ್ತಿದ್ದಾರೆ', seconds: 'ಸೆ', degrees: '°',
    interp: (e) => {
      const s = e.severityLevel ? ({ LOW: 'ಕಡಿಮೆ', MEDIUM: 'ಮಧ್ಯಮ', HIGH: 'ತೀವ್ರ' })[e.severityLevel] : null;
      const what = e.kind === 'crash' ? 'ಅಪಘಾತ' : 'ಬೀಳುವಿಕೆ';
      const a = s ? `${s} ಆಘಾತದ ${what}` : `${what} ವರದಿಯಾಗಿದೆ`;
      return e.stillnessSec != null ? `${a}, ನಂತರ ${Math.round(e.stillnessSec)} ಸೆಕೆಂಡು ಚಲನೆ ಇಲ್ಲ` : a;
    },
    summaryFallback: (d) => `${d.type}, ಸಮಯ ${d.time}. ${d.severity ? 'ತೀವ್ರತೆ ' + d.severity + '. ' : ''}${d.status}. ಕೊನೆಯ ಸ್ಥಳ: ${d.address}. ಹತ್ತಿರದ ಆಸ್ಪತ್ರೆ: ${d.hospital}.`,
    guide: {
      common: ['ಈಗಲೇ 112 / 108 ಗೆ ಕರೆ ಮಾಡಿ.'],
      fall: ['ಕುತ್ತಿಗೆ, ಬೆನ್ನು ಅಥವಾ ತಲೆಗೆ ಗಾಯದ ಸಂಶಯವಿದ್ದರೆ ವ್ಯಕ್ತಿಯನ್ನು ಅಲುಗಾಡಿಸಬೇಡಿ.', 'ಅವರು ಎಚ್ಚರವಾಗಿದ್ದಾರೆಯೇ, ಉಸಿರಾಡುತ್ತಿದ್ದಾರೆಯೇ ಎಂದು ನೋಡಿ.', 'ಸ್ವಚ್ಛ ಬಟ್ಟೆಯಿಂದ ಗಟ್ಟಿಯಾಗಿ ಒತ್ತಿ ರಕ್ತಸ್ರಾವ ತಡೆಯಿರಿ.', 'ಸಹಾಯ ಬರುವವರೆಗೆ ಅವರನ್ನು ಬೆಚ್ಚಗೆ ಮತ್ತು ಸ್ಥಿರವಾಗಿ ಇರಿಸಿ.'],
      crash: ['ವಾಹನದ ಎಂಜಿನ್ ಆಫ್ ಮಾಡಿ; ಟ್ರಾಫಿಕ್ ಮತ್ತು ಇಂಧನದಿಂದ ದೂರವಿರಿ.', 'ತಕ್ಷಣದ ಅಪಾಯವಿಲ್ಲದಿದ್ದರೆ ಹೆಲ್ಮೆಟ್ ತೆಗೆಯಬೇಡಿ, ವ್ಯಕ್ತಿಯನ್ನು ಸರಿಸಬೇಡಿ.', 'ಉಸಿರು ಪರೀಕ್ಷಿಸಿ; ಉಸಿರಿಲ್ಲದಿದ್ದರೆ ಮತ್ತು CPR ಗೊತ್ತಿದ್ದರೆ ಆರಂಭಿಸಿ.', 'ಹೆಚ್ಚು ರಕ್ತಸ್ರಾವವಿದ್ದರೆ ಗಟ್ಟಿಯಾಗಿ ಒತ್ತಿ.'],
      sos: ['ಸುರಕ್ಷಿತವಾಗಿರಿ; ಯಾರನ್ನೂ ಎದುರಿಸಬೇಡಿ.', 'ಸ್ಥಳ, ಜನರು ಮತ್ತು ವಾಹನಗಳನ್ನು ಗಮನಿಸಿ.', 'ಫೋನ್ ಲೈನ್ ತೆರೆದಿಡಿ ಮತ್ತು ಪೊಲೀಸರಿಗೆ ಸ್ಥಳ ಹಂಚಿ.', 'ಸಾಧ್ಯವಾದರೆ ಸುರಕ್ಷಿತ, ಜನನಿಬಿಡ ಸ್ಥಳಕ್ಕೆ ಹೋಗಿ.'],
      journey_alert: ['ವ್ಯಕ್ತಿಗೆ ಕರೆ ಮಾಡಲು ಪ್ರಯತ್ನಿಸಿ; ಸುರಕ್ಷಿತರೆಂದು ಭಾವಿಸಬೇಡಿ.', 'ಸಂಪರ್ಕ ಸಾಧ್ಯವಾಗದಿದ್ದರೆ ಕೊನೆಯ ಸ್ಥಳವನ್ನು ಪೊಲೀಸರಿಗೆ ನೀಡಿ.', 'ಇಲ್ಲಿರುವ ವಾಹನ ಮತ್ತು ಚಾಲಕರ ವಿವರ ಗಮನಿಸಿ.', 'ಫೋನ್ ಕೊನೆಯದಾಗಿ ಎಲ್ಲಿ ಚಲಿಸಿತು ಎಂದು ಮಾರ್ಗ ನೋಡಿ.'],
    },
    journey: { title: 'ಸುರಕ್ಷಿತ ಪ್ರಯಾಣ', to: 'ಗಮ್ಯ', eta: 'ತಲುಪುವ ಸಮಯ', status: { on_route: 'ಮಾರ್ಗದಲ್ಲಿ', deviated: 'ಮಾರ್ಗ ತಪ್ಪಿದೆ', stopped: 'ನಿಂತಿದ್ದಾರೆ', delayed: 'ತಡ', arrived: 'ತಲುಪಿದ್ದಾರೆ', alert: 'ಎಚ್ಚರಿಕೆ' }, callName: (n) => `${n} ಗೆ ಕರೆ ಮಾಡಿ`, cab: 'ಕ್ಯಾಬ್ ವಿವರ', vehicle: 'ವಾಹನ', driver: 'ಚಾಲಕ', battery: 'ಬ್ಯಾಟರಿ', offline: 'ಫೋನ್ ಆಫ್‌ಲೈನ್' },
    sms: {
      journeyStart: (n, place, eta, link) => `${n} ${place} ಗೆ ಪ್ರಯಾಣ ಆರಂಭಿಸಿದ್ದಾರೆ. ತಲುಪುವ ಸಮಯ ${eta}. ಟ್ರ್ಯಾಕ್: ${link}`,
      arrived: (n) => `${n} ಸುರಕ್ಷಿತವಾಗಿ ತಲುಪಿದ್ದಾರೆ.`,
      watchdog: (n, mins, link, pos) => `ಎಚ್ಚರಿಕೆ: ${n} ಅವರ ಫೋನ್‌ನಿಂದ ${mins} ನಿಮಿಷಗಳಿಂದ ಅಪ್‌ಡೇಟ್ ಇಲ್ಲ. ಕೊನೆಯ ಸ್ಥಳ: ${pos}. ಟ್ರ್ಯಾಕ್: ${link}`,
      overdue: (n, link, pos) => `ಎಚ್ಚರಿಕೆ: ${n} ಇನ್ನೂ ತಲುಪಿಲ್ಲ. ಕೊನೆಯ ಸ್ಥಳ: ${pos}. ಟ್ರ್ಯಾಕ್: ${link}`,
    },
  },
};
const LANGS = Object.keys(L);
function t(lang) { return L[LANGS.includes(lang) ? lang : 'en']; }
module.exports = { t, LANGS };
