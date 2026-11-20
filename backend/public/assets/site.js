(function(){
var D={
 en:{nav_home:'Home',nav_chat:'Ask VERA',nav_dash:'Caregiver dashboard',nav_priv:'Privacy',
  hero_h:'Help finds you within seconds.',hero_p:'A fall, an attack or a missed journey happens, and within seconds the right people know where you are, who you are, and where the nearest help is.',
  dl:'Download APK',dl_note:'Android 8.0+ · works with the screen off and the app closed',
  how_h:'How it works',how1:'Fall and crash protection watches your phone sensors in the background and asks "Are you okay?"',how2:'Silent SOS from a shake, widget, tile or notification alerts your trusted contacts without any sound.',how3:'Safe Journey tells your people your route and ETA, and a server-side dead-man\'s switch alerts them even if your phone dies.',how4:'Responders and family open one link: a 10-second Responder Brief with your blood group, allergies, location and the nearest hospitals.',
  inst_h:'Install on your phone',inst1:'Tap Download APK and open the file.',inst2:'If asked, allow "Install unknown apps" for your browser.',inst3:'If Google Play Protect warns about an unrecognised app, choose "Install anyway" (this is a hackathon build, not on the Play Store).',inst4:'Open VERA Guard and complete the setup checklist (location, notifications, SMS, battery).',
  apk_missing:'The APK has not been uploaded to this server yet.',
  notice:'VERA Guard supplements, and does not replace, emergency services. In an emergency call 112 (or 108 for an ambulance).',
  shots_h:'The alert screen',shots_p:'Loud alarm, spoken prompt in your language, and a countdown. Tap I\'M OK to cancel.',ok:'I\'M OK',
  chat_h:'Ask VERA',chat_p:'First-aid and safety help in English, Hindi or Kannada. Informational only, not a diagnosis.',chat_ph:'Type your question…',send:'Send',
  c1:'I fell, what should I do?',c2:'Someone is bleeding',c3:'CPR steps',chat_err:'The assistant is offline. For emergencies call 112 / 108.',
  dash_h:'Caregiver dashboard',dash_p:'Enter the caregiver code shown in the VERA Guard app (Profile → Caregiver dashboard).',dash_go:'Open',dash_sessions:'Emergencies',dash_journeys:'Journeys',when:'When',type:'Type',status:'Status',dest:'Destination',open:'Open',none:'Nothing yet.',
  priv_h:'Privacy',
  priv:['What is collected: only what you enable in the app. Emergency events (sensor summary, location trail), journey routes and positions, and the profile fields you chose to share (blood group is on by default; medications are off by default).','Who can see it: anyone holding the long, random tracking link (the people you alert and responders they forward it to). Links are unguessable, need no login, and can be ended by you at any time.','How long it is kept: tracking sessions and journeys are deleted automatically after 72 hours. Audio recordings (optional, off by default) are encrypted on your phone before any upload.','Your controls: per-field sharing toggles, a preview of exactly what responders see, "Export my data" and "Delete all my data" in the app (this also deletes server-side sessions and journeys).','Chat: messages are sent to our server only to be forwarded to the AI provider; we do not store them. Do not enter information you do not want processed by an AI service.','Demo mode never contacts 112, 108 or the police.'],
  loading:'Loading…',notfound:'This link is not valid or has expired.',print:'Print',updated:'Updated'},
 hi:{nav_home:'होम',nav_chat:'VERA से पूछें',nav_dash:'केयरगिवर डैशबोर्ड',nav_priv:'गोपनीयता',
  hero_h:'मदद सेकंडों में आप तक पहुँचती है।',hero_p:'गिरना, हमला या छूटी हुई यात्रा — और कुछ ही सेकंड में सही लोगों को पता चल जाता है कि आप कहाँ हैं, कौन हैं और सबसे नज़दीकी मदद कहाँ है।',
  dl:'APK डाउनलोड करें',dl_note:'Android 8.0+ · स्क्रीन बंद और ऐप बंद होने पर भी काम करता है',
  how_h:'यह कैसे काम करता है',how1:'गिरने और दुर्घटना की सुरक्षा बैकग्राउंड में फ़ोन के सेंसर देखती है और पूछती है "क्या आप ठीक हैं?"',how2:'शेक, विजेट, टाइल या नोटिफ़िकेशन से साइलेंट SOS बिना आवाज़ आपके भरोसेमंद संपर्कों को सूचित करता है।',how3:'सेफ़ जर्नी आपके लोगों को रूट और ETA बताती है, और सर्वर की डेड-मैन स्विच फ़ोन बंद होने पर भी उन्हें अलर्ट करती है।',how4:'रेस्पॉन्डर और परिवार एक लिंक खोलते हैं: 10 सेकंड का रेस्पॉन्डर ब्रीफ़ — रक्त समूह, एलर्जी, स्थान और नज़दीकी अस्पताल।',
  inst_h:'अपने फ़ोन पर इंस्टॉल करें',inst1:'APK डाउनलोड करें दबाएँ और फ़ाइल खोलें।',inst2:'पूछे जाने पर अपने ब्राउज़र के लिए "अज्ञात ऐप इंस्टॉल करें" की अनुमति दें।',inst3:'अगर Google Play Protect चेतावनी दे, तो "फिर भी इंस्टॉल करें" चुनें (यह हैकथॉन बिल्ड है, Play Store पर नहीं है)।',inst4:'VERA Guard खोलें और सेटअप चेकलिस्ट पूरी करें (लोकेशन, नोटिफ़िकेशन, SMS, बैटरी)।',
  apk_missing:'APK अभी इस सर्वर पर अपलोड नहीं हुआ है।',
  notice:'VERA Guard आपातकालीन सेवाओं का पूरक है, विकल्प नहीं। आपातकाल में 112 (या एम्बुलेंस के लिए 108) पर कॉल करें।',
  shots_h:'अलर्ट स्क्रीन',shots_p:'तेज़ अलार्म, आपकी भाषा में बोला गया सवाल और काउंटडाउन। रद्द करने के लिए मैं ठीक हूँ दबाएँ।',ok:'मैं ठीक हूँ',
  chat_h:'VERA से पूछें',chat_p:'अंग्रेज़ी, हिंदी या कन्नड़ में प्राथमिक उपचार और सुरक्षा सहायता। केवल जानकारी के लिए, निदान नहीं।',chat_ph:'अपना सवाल लिखें…',send:'भेजें',
  c1:'मैं गिर गया, क्या करूँ?',c2:'किसी का खून बह रहा है',c3:'CPR के चरण',chat_err:'असिस्टेंट ऑफ़लाइन है। आपातकाल में 112 / 108 पर कॉल करें।',
  dash_h:'केयरगिवर डैशबोर्ड',dash_p:'VERA Guard ऐप में दिखा केयरगिवर कोड डालें (प्रोफ़ाइल → केयरगिवर डैशबोर्ड)।',dash_go:'खोलें',dash_sessions:'आपात घटनाएँ',dash_journeys:'यात्राएँ',when:'कब',type:'प्रकार',status:'स्थिति',dest:'गंतव्य',open:'खोलें',none:'अभी कुछ नहीं।',
  priv_h:'गोपनीयता',
  priv:['क्या इकट्ठा होता है: केवल वही जो आप ऐप में चालू करते हैं। आपात घटनाएँ (सेंसर सारांश, लोकेशन ट्रेल), यात्रा के रूट और स्थान, और आपके चुने प्रोफ़ाइल फ़ील्ड (रक्त समूह डिफ़ॉल्ट रूप से चालू, दवाइयाँ बंद)।','कौन देख सकता है: जिसके पास लंबा, यादृच्छिक ट्रैकिंग लिंक है (जिन्हें आप अलर्ट करते हैं और वे जिन रेस्पॉन्डर को लिंक भेजते हैं)। लिंक अनुमान से परे हैं, लॉगिन नहीं चाहिए, और आप कभी भी उन्हें समाप्त कर सकते हैं।','कितने समय रखा जाता है: ट्रैकिंग सेशन और यात्राएँ 72 घंटे बाद अपने-आप हट जाती हैं। ऑडियो रिकॉर्डिंग (वैकल्पिक, डिफ़ॉल्ट रूप से बंद) अपलोड से पहले फ़ोन पर एन्क्रिप्ट होती है।','आपके नियंत्रण: हर फ़ील्ड का शेयरिंग टॉगल, रेस्पॉन्डर को जो दिखेगा उसका प्रीव्यू, ऐप में "मेरा डेटा एक्सपोर्ट करें" और "मेरा सारा डेटा हटाएँ" (इससे सर्वर के सेशन और यात्राएँ भी हटती हैं)।','चैट: संदेश केवल AI प्रदाता को भेजने के लिए हमारे सर्वर से गुज़रते हैं; हम उन्हें सहेजते नहीं। ऐसी जानकारी न डालें जिसे आप AI सेवा से प्रोसेस नहीं करवाना चाहते।','डेमो मोड कभी 112, 108 या पुलिस को संपर्क नहीं करता।'],
  loading:'लोड हो रहा है…',notfound:'यह लिंक अमान्य है या समाप्त हो गया है।',print:'प्रिंट',updated:'अपडेट'},
 kn:{nav_home:'ಮುಖಪುಟ',nav_chat:'VERA ಕೇಳಿ',nav_dash:'ಆರೈಕೆದಾರರ ಡ್ಯಾಶ್‌ಬೋರ್ಡ್',nav_priv:'ಗೌಪ್ಯತೆ',
  hero_h:'ಸಹಾಯ ಸೆಕೆಂಡುಗಳಲ್ಲಿ ನಿಮ್ಮನ್ನು ತಲುಪುತ್ತದೆ.',hero_p:'ಬೀಳುವಿಕೆ, ದಾಳಿ ಅಥವಾ ತಪ್ಪಿದ ಪ್ರಯಾಣ ಸಂಭವಿಸಿದಾಗ, ಕೆಲವೇ ಸೆಕೆಂಡುಗಳಲ್ಲಿ ಸರಿಯಾದ ಜನರಿಗೆ ನೀವು ಎಲ್ಲಿದ್ದೀರಿ, ಯಾರು ಮತ್ತು ಹತ್ತಿರದ ಸಹಾಯ ಎಲ್ಲಿದೆ ಎಂಬುದು ತಿಳಿಯುತ್ತದೆ.',
  dl:'APK ಡೌನ್‌ಲೋಡ್',dl_note:'Android 8.0+ · ಸ್ಕ್ರೀನ್ ಆಫ್ ಮತ್ತು ಆ್ಯಪ್ ಮುಚ್ಚಿದ್ದರೂ ಕೆಲಸ ಮಾಡುತ್ತದೆ',
  how_h:'ಇದು ಹೇಗೆ ಕೆಲಸ ಮಾಡುತ್ತದೆ',how1:'ಬೀಳುವಿಕೆ ಮತ್ತು ಅಪಘಾತ ರಕ್ಷಣೆ ಹಿನ್ನೆಲೆಯಲ್ಲಿ ಫೋನ್ ಸೆನ್ಸರ್‌ಗಳನ್ನು ಗಮನಿಸಿ "ನೀವು ಚೆನ್ನಾಗಿದ್ದೀರಾ?" ಎಂದು ಕೇಳುತ್ತದೆ.',how2:'ಶೇಕ್, ವಿಜೆಟ್, ಟೈಲ್ ಅಥವಾ ನೋಟಿಫಿಕೇಶನ್‌ನಿಂದ ಮೌನ SOS ಶಬ್ದವಿಲ್ಲದೆ ನಿಮ್ಮ ವಿಶ್ವಾಸಾರ್ಹ ಸಂಪರ್ಕಗಳಿಗೆ ತಿಳಿಸುತ್ತದೆ.',how3:'ಸೇಫ್ ಜರ್ನಿ ನಿಮ್ಮ ಮಾರ್ಗ ಮತ್ತು ETA ತಿಳಿಸುತ್ತದೆ; ಸರ್ವರ್‌ನ ಡೆಡ್-ಮ್ಯಾನ್ ಸ್ವಿಚ್ ಫೋನ್ ಆಫ್ ಆದರೂ ಅವರನ್ನು ಎಚ್ಚರಿಸುತ್ತದೆ.',how4:'ಸಹಾಯಕರು ಮತ್ತು ಕುಟುಂಬ ಒಂದು ಲಿಂಕ್ ತೆರೆಯುತ್ತಾರೆ: 10 ಸೆಕೆಂಡಿನ ರೆಸ್ಪಾಂಡರ್ ಬ್ರೀಫ್ — ರಕ್ತದ ಗುಂಪು, ಅಲರ್ಜಿ, ಸ್ಥಳ ಮತ್ತು ಹತ್ತಿರದ ಆಸ್ಪತ್ರೆಗಳು.',
  inst_h:'ನಿಮ್ಮ ಫೋನ್‌ನಲ್ಲಿ ಇನ್‌ಸ್ಟಾಲ್ ಮಾಡಿ',inst1:'APK ಡೌನ್‌ಲೋಡ್ ಒತ್ತಿ ಫೈಲ್ ತೆರೆಯಿರಿ.',inst2:'ಕೇಳಿದರೆ ನಿಮ್ಮ ಬ್ರೌಸರ್‌ಗೆ "ಅಜ್ಞಾತ ಆ್ಯಪ್‌ಗಳ ಇನ್‌ಸ್ಟಾಲ್" ಅನುಮತಿಸಿ.',inst3:'Google Play Protect ಎಚ್ಚರಿಸಿದರೆ "ಹೇಗಾದರೂ ಇನ್‌ಸ್ಟಾಲ್ ಮಾಡಿ" ಆಯ್ಕೆಮಾಡಿ (ಇದು ಹ್ಯಾಕಥಾನ್ ಬಿಲ್ಡ್, Play Store ನಲ್ಲಿಲ್ಲ).',inst4:'VERA Guard ತೆರೆದು ಸೆಟಪ್ ಚೆಕ್‌ಲಿಸ್ಟ್ ಪೂರ್ಣಗೊಳಿಸಿ (ಸ್ಥಳ, ನೋಟಿಫಿಕೇಶನ್, SMS, ಬ್ಯಾಟರಿ).',
  apk_missing:'APK ಇನ್ನೂ ಈ ಸರ್ವರ್‌ಗೆ ಅಪ್‌ಲೋಡ್ ಆಗಿಲ್ಲ.',
  notice:'VERA Guard ತುರ್ತು ಸೇವೆಗಳಿಗೆ ಪೂರಕ, ಬದಲಿಯಲ್ಲ. ತುರ್ತು ಸಂದರ್ಭದಲ್ಲಿ 112 (ಆಂಬ್ಯುಲೆನ್ಸ್‌ಗೆ 108) ಗೆ ಕರೆ ಮಾಡಿ.',
  shots_h:'ಎಚ್ಚರಿಕೆ ಪರದೆ',shots_p:'ಜೋರಾದ ಅಲಾರಂ, ನಿಮ್ಮ ಭಾಷೆಯಲ್ಲಿ ಮಾತಿನ ಪ್ರಶ್ನೆ ಮತ್ತು ಕೌಂಟ್‌ಡೌನ್. ರದ್ದುಮಾಡಲು ನಾನು ಚೆನ್ನಾಗಿದ್ದೇನೆ ಒತ್ತಿ.',ok:'ನಾನು ಚೆನ್ನಾಗಿದ್ದೇನೆ',
  chat_h:'VERA ಕೇಳಿ',chat_p:'ಇಂಗ್ಲಿಷ್, ಹಿಂದಿ ಅಥವಾ ಕನ್ನಡದಲ್ಲಿ ಪ್ರಥಮ ಚಿಕಿತ್ಸೆ ಮತ್ತು ಸುರಕ್ಷತಾ ಸಹಾಯ. ಮಾಹಿತಿಗಾಗಿ ಮಾತ್ರ, ರೋಗನಿರ್ಣಯವಲ್ಲ.',chat_ph:'ನಿಮ್ಮ ಪ್ರಶ್ನೆ ಬರೆಯಿರಿ…',send:'ಕಳುಹಿಸಿ',
  c1:'ನಾನು ಬಿದ್ದೆ, ಏನು ಮಾಡಲಿ?',c2:'ಯಾರಿಗೋ ರಕ್ತಸ್ರಾವವಾಗುತ್ತಿದೆ',c3:'CPR ಹಂತಗಳು',chat_err:'ಸಹಾಯಕ ಆಫ್‌ಲೈನ್ ಇದೆ. ತುರ್ತು ಸಂದರ್ಭದಲ್ಲಿ 112 / 108 ಗೆ ಕರೆ ಮಾಡಿ.',
  dash_h:'ಆರೈಕೆದಾರರ ಡ್ಯಾಶ್‌ಬೋರ್ಡ್',dash_p:'VERA Guard ಆ್ಯಪ್‌ನಲ್ಲಿ ತೋರಿಸಿದ ಕೋಡ್ ನಮೂದಿಸಿ (ಪ್ರೊಫೈಲ್ → ಆರೈಕೆದಾರರ ಡ್ಯಾಶ್‌ಬೋರ್ಡ್).',dash_go:'ತೆರೆಯಿರಿ',dash_sessions:'ತುರ್ತು ಘಟನೆಗಳು',dash_journeys:'ಪ್ರಯಾಣಗಳು',when:'ಯಾವಾಗ',type:'ವಿಧ',status:'ಸ್ಥಿತಿ',dest:'ಗಮ್ಯ',open:'ತೆರೆಯಿರಿ',none:'ಇನ್ನೂ ಏನೂ ಇಲ್ಲ.',
  priv_h:'ಗೌಪ್ಯತೆ',
  priv:['ಏನು ಸಂಗ್ರಹವಾಗುತ್ತದೆ: ನೀವು ಆ್ಯಪ್‌ನಲ್ಲಿ ಆನ್ ಮಾಡಿದ್ದು ಮಾತ್ರ. ತುರ್ತು ಘಟನೆಗಳು (ಸೆನ್ಸರ್ ಸಾರಾಂಶ, ಸ್ಥಳದ ಹಾದಿ), ಪ್ರಯಾಣದ ಮಾರ್ಗ ಮತ್ತು ಸ್ಥಾನಗಳು, ಮತ್ತು ನೀವು ಹಂಚಲು ಆರಿಸಿದ ಪ್ರೊಫೈಲ್ ಕ್ಷೇತ್ರಗಳು (ರಕ್ತದ ಗುಂಪು ಡೀಫಾಲ್ಟ್ ಆನ್, ಔಷಧಗಳು ಆಫ್).','ಯಾರು ನೋಡಬಹುದು: ಉದ್ದವಾದ, ಯಾದೃಚ್ಛಿಕ ಟ್ರ್ಯಾಕಿಂಗ್ ಲಿಂಕ್ ಹೊಂದಿರುವವರು (ನೀವು ಎಚ್ಚರಿಸಿದವರು ಮತ್ತು ಅವರು ಲಿಂಕ್ ಕಳುಹಿಸಿದ ಸಹಾಯಕರು). ಲಿಂಕ್‌ಗಳನ್ನು ಊಹಿಸಲಾಗದು, ಲಾಗಿನ್ ಬೇಕಿಲ್ಲ, ನೀವು ಯಾವಾಗ ಬೇಕಾದರೂ ಮುಗಿಸಬಹುದು.','ಎಷ್ಟು ಕಾಲ ಇಡಲಾಗುತ್ತದೆ: ಟ್ರ್ಯಾಕಿಂಗ್ ಸೆಷನ್‌ಗಳು ಮತ್ತು ಪ್ರಯಾಣಗಳು 72 ಗಂಟೆಗಳ ನಂತರ ತಾನಾಗಿ ಅಳಿಸಲ್ಪಡುತ್ತವೆ. ಆಡಿಯೋ ರೆಕಾರ್ಡಿಂಗ್ (ಐಚ್ಛಿಕ, ಡೀಫಾಲ್ಟ್ ಆಫ್) ಅಪ್‌ಲೋಡ್‌ಗೂ ಮುನ್ನ ಫೋನ್‌ನಲ್ಲಿ ಎನ್‌ಕ್ರಿಪ್ಟ್ ಆಗುತ್ತದೆ.','ನಿಮ್ಮ ನಿಯಂತ್ರಣ: ಪ್ರತಿ ಕ್ಷೇತ್ರದ ಹಂಚಿಕೆ ಟಾಗಲ್, ಸಹಾಯಕರಿಗೆ ಕಾಣುವುದರ ಪೂರ್ವವೀಕ್ಷಣೆ, ಆ್ಯಪ್‌ನಲ್ಲಿ "ನನ್ನ ಡೇಟಾ ರಫ್ತು" ಮತ್ತು "ನನ್ನ ಎಲ್ಲಾ ಡೇಟಾ ಅಳಿಸಿ" (ಇದು ಸರ್ವರ್‌ನ ಸೆಷನ್ ಮತ್ತು ಪ್ರಯಾಣಗಳನ್ನೂ ಅಳಿಸುತ್ತದೆ).','ಚಾಟ್: ಸಂದೇಶಗಳು AI ಪೂರೈಕೆದಾರರಿಗೆ ರವಾನಿಸಲು ಮಾತ್ರ ನಮ್ಮ ಸರ್ವರ್ ಮೂಲಕ ಹೋಗುತ್ತವೆ; ನಾವು ಅವನ್ನು ಉಳಿಸುವುದಿಲ್ಲ. AI ಸೇವೆ ಸಂಸ್ಕರಿಸಬಾರದೆಂದು ಬಯಸುವ ಮಾಹಿತಿಯನ್ನು ನಮೂದಿಸಬೇಡಿ.','ಡೆಮೊ ಮೋಡ್ ಎಂದಿಗೂ 112, 108 ಅಥವಾ ಪೊಲೀಸರನ್ನು ಸಂಪರ್ಕಿಸುವುದಿಲ್ಲ.'],
  loading:'ಲೋಡ್ ಆಗುತ್ತಿದೆ…',notfound:'ಈ ಲಿಂಕ್ ಅಮಾನ್ಯ ಅಥವಾ ಅವಧಿ ಮುಗಿದಿದೆ.',print:'ಮುದ್ರಿಸಿ',updated:'ಅಪ್‌ಡೇಟ್'}
};
var lang=localStorage.getItem('lang')||((navigator.language||'en').slice(0,2));if(!D[lang])lang='en';
window.VERA={lang:function(){return lang},t:function(k){return (D[lang]||D.en)[k]},D:D,
 setLang:function(l){lang=l;try{localStorage.setItem('lang',l)}catch(e){}apply();document.dispatchEvent(new Event('langchange'))}};
function apply(){document.documentElement.lang=lang;
 document.querySelectorAll('[data-i18n]').forEach(function(e){var v=VERA.t(e.getAttribute('data-i18n'));if(v!=null)e.textContent=v});
 document.querySelectorAll('[data-i18n-ph]').forEach(function(e){e.placeholder=VERA.t(e.getAttribute('data-i18n-ph'))});
 document.querySelectorAll('.lang button').forEach(function(b){b.classList.toggle('on',b.dataset.l===lang)})}
document.addEventListener('DOMContentLoaded',function(){
 var h=document.getElementById('top');
 if(h)h.innerHTML='<a class="brand" href="/">VERA <b>Guard</b></a><nav><a href="/" data-i18n="nav_home"></a><a href="/chat" data-i18n="nav_chat"></a><a href="/dashboard" data-i18n="nav_dash"></a><a href="/privacy" data-i18n="nav_priv"></a></nav><span class="lang"><button data-l="en">EN</button> <button data-l="hi">हिं</button> <button data-l="kn">ಕನ್ನ</button></span>';
 document.querySelectorAll('.lang button').forEach(function(b){b.onclick=function(){VERA.setLang(b.dataset.l)}});
 apply();document.dispatchEvent(new Event('langchange'));
});
})();
