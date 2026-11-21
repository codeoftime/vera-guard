#!/usr/bin/env python3
"""Generate values/strings.xml, values-hi/strings.xml, values-kn/strings.xml and verify that every
R.string.* / @string/* referenced in the app has an entry in all three languages (fails otherwise)."""
import re, sys, pathlib
from xml.sax.saxutils import escape

ROOT = pathlib.Path(__file__).resolve().parent.parent / "android" / "app" / "src" / "main"
S = {}
def a(k, en, hi, kn): S[k] = (en, hi, kn)

a("app_name","VERA Guard","VERA Guard","VERA Guard")
a("tile_label","Silent SOS","साइलेंट SOS","ಮೌನ SOS")
a("channel_guard","Protection running","सुरक्षा चालू है","ರಕ್ಷಣೆ ಚಾಲನೆಯಲ್ಲಿದೆ")
a("channel_alert","Emergency alerts","आपातकालीन अलर्ट","ತುರ್ತು ಎಚ್ಚರಿಕೆಗಳು")
a("channel_quiet","Quiet status","शांत स्थिति","ಶಾಂತ ಸ್ಥಿತಿ")
a("notif_title","VERA Guard is protecting you","VERA Guard आपकी सुरक्षा कर रहा है","VERA Guard ನಿಮ್ಮನ್ನು ರಕ್ಷಿಸುತ್ತಿದೆ")
a("notif_text","Fall detection and SOS are active","गिरने की पहचान और SOS चालू हैं","ಬೀಳುವಿಕೆ ಪತ್ತೆ ಮತ್ತು SOS ಸಕ್ರಿಯವಾಗಿವೆ")
a("notif_sos_action","SOS","SOS","SOS")
a("notif_discreet_title","Device service","डिवाइस सेवा","ಸಾಧನ ಸೇವೆ")
a("notif_discreet_text","Running","चालू है","ಚಾಲನೆಯಲ್ಲಿದೆ")
a("alert_notif_title","Possible fall detected","संभावित गिरना पहचाना गया","ಬೀಳುವಿಕೆ ಪತ್ತೆಯಾಗಿರಬಹುದು")
a("alert_notif_text","Tap to confirm you are okay","ठीक हैं, यह बताने के लिए टैप करें","ನೀವು ಚೆನ್ನಾಗಿದ್ದೀರಿ ಎಂದು ದೃಢೀಕರಿಸಲು ಟ್ಯಾಪ್ ಮಾಡಿ")
a("tts_fall","A fall was detected. Are you okay? Say or press I am okay to cancel.","गिरना पहचाना गया। क्या आप ठीक हैं? रद्द करने के लिए मैं ठीक हूँ बोलें या दबाएँ।","ಬೀಳುವಿಕೆ ಪತ್ತೆಯಾಗಿದೆ. ನೀವು ಚೆನ್ನಾಗಿದ್ದೀರಾ? ರದ್ದುಮಾಡಲು ನಾನು ಚೆನ್ನಾಗಿದ್ದೇನೆ ಎಂದು ಹೇಳಿ ಅಥವಾ ಒತ್ತಿ.")
a("tts_checkin","Safety check. Are you okay?","सुरक्षा जाँच। क्या आप ठीक हैं?","ಸುರಕ್ಷತಾ ಪರಿಶೀಲನೆ. ನೀವು ಚೆನ್ನಾಗಿದ್ದೀರಾ?")
a("checkin_title","Are you okay?","क्या आप ठीक हैं?","ನೀವು ಚೆನ್ನಾಗಿದ್ದೀರಾ?")
a("checkin_text","Your journey looks unusual. Please confirm.","आपकी यात्रा असामान्य लग रही है। कृपया पुष्टि करें।","ನಿಮ್ಮ ಪ್ರಯಾಣ ಅಸಾಮಾನ್ಯವಾಗಿ ಕಾಣುತ್ತಿದೆ. ದಯವಿಟ್ಟು ದೃಢೀಕರಿಸಿ.")
a("journey_arrived","You have arrived safely. Followers were notified.","आप सुरक्षित पहुँच गए। फ़ॉलोअर्स को सूचित किया गया।","ನೀವು ಸುರಕ್ಷಿತವಾಗಿ ತಲುಪಿದ್ದೀರಿ. ಅನುಸರಿಸುವವರಿಗೆ ತಿಳಿಸಲಾಗಿದೆ.")
a("journey_low_battery","Battery is low. Your followers were told.","बैटरी कम है। आपके फ़ॉलोअर्स को बता दिया गया।","ಬ್ಯಾಟರಿ ಕಡಿಮೆಯಾಗಿದೆ. ನಿಮ್ಮ ಅನುಸರಿಸುವವರಿಗೆ ತಿಳಿಸಲಾಗಿದೆ.")
a("sim_dead_msg","Simulating a dead phone: location updates stopped. The server watchdog should alert your followers.","फ़ोन बंद होने का अनुकरण: लोकेशन अपडेट रुक गए। सर्वर वॉचडॉग आपके फ़ॉलोअर्स को अलर्ट करेगा।","ಫೋನ್ ಸ್ಥಗಿತದ ಅನುಕರಣೆ: ಸ್ಥಳ ಅಪ್‌ಡೇಟ್‌ಗಳು ನಿಂತಿವೆ. ಸರ್ವರ್ ವಾಚ್‌ಡಾಗ್ ನಿಮ್ಮ ಅನುಸರಿಸುವವರಿಗೆ ಎಚ್ಚರಿಕೆ ನೀಡುತ್ತದೆ.")
a("sos_cancelled","SOS cancelled","SOS रद्द किया गया","SOS ರದ್ದುಗೊಳಿಸಲಾಗಿದೆ")
a("demo_banner","DEMO MODE: no real emergency calls are placed","डेमो मोड: कोई वास्तविक आपातकालीन कॉल नहीं होगी","ಡೆಮೊ ಮೋಡ್: ಯಾವುದೇ ನಿಜವಾದ ತುರ್ತು ಕರೆ ಮಾಡಲಾಗುವುದಿಲ್ಲ")
a("demo_hint","Messages go only to the demo phone number. 112 and 108 are never called.","संदेश केवल डेमो नंबर पर जाते हैं। 112 और 108 पर कभी कॉल नहीं होती।","ಸಂದೇಶಗಳು ಡೆಮೊ ಸಂಖ್ಯೆಗೆ ಮಾತ್ರ ಹೋಗುತ್ತವೆ. 112 ಮತ್ತು 108 ಗೆ ಎಂದಿಗೂ ಕರೆ ಮಾಡುವುದಿಲ್ಲ.")
a("crash_detected","Possible crash detected","संभावित दुर्घटना पहचानी गई","ಸಂಭವನೀಯ ಅಪಘಾತ ಪತ್ತೆಯಾಗಿದೆ")
a("fall_detected","Possible fall detected","संभावित गिरना पहचाना गया","ಸಂಭವನೀಯ ಬೀಳುವಿಕೆ ಪತ್ತೆಯಾಗಿದೆ")
a("are_you_okay","Are you okay?","क्या आप ठीक हैं?","ನೀವು ಚೆನ್ನಾಗಿದ್ದೀರಾ?")
a("alerting_in","Alerting your contacts in %d s","%d सेकंड में आपके संपर्कों को अलर्ट किया जाएगा","%d ಸೆಕೆಂಡ್‌ನಲ್ಲಿ ನಿಮ್ಮ ಸಂಪರ್ಕಗಳಿಗೆ ಎಚ್ಚರಿಕೆ ಕಳುಹಿಸಲಾಗುತ್ತದೆ")
a("i_need_help","I NEED HELP","मुझे मदद चाहिए","ನನಗೆ ಸಹಾಯ ಬೇಕು")
a("alerting_contacts","Alerting contacts…","संपर्कों को अलर्ट कर रहे हैं…","ಸಂಪರ್ಕಗಳಿಗೆ ಎಚ್ಚರಿಕೆ ಕಳುಹಿಸಲಾಗುತ್ತಿದೆ…")
a("help_alerted","Your contacts have been alerted. Stay where you are if it is safe.","आपके संपर्कों को अलर्ट कर दिया गया है। यदि सुरक्षित हो तो वहीं रहें।","ನಿಮ್ಮ ಸಂಪರ್ಕಗಳಿಗೆ ಎಚ್ಚರಿಕೆ ಕಳುಹಿಸಲಾಗಿದೆ. ಸುರಕ್ಷಿತವಾಗಿದ್ದರೆ ಅಲ್ಲಿಯೇ ಇರಿ.")
a("responder_on_way","Help is being arranged","मदद की व्यवस्था की जा रही है","ಸಹಾಯದ ವ್ಯವಸ್ಥೆ ಮಾಡಲಾಗುತ್ತಿದೆ")
a("call_number","CALL %s","%s पर कॉल करें","%s ಗೆ ಕರೆ ಮಾಡಿ")
a("cancelled_ok","Cancelled. Glad you are okay.","रद्द किया गया। अच्छा है कि आप ठीक हैं।","ರದ್ದುಗೊಳಿಸಲಾಗಿದೆ. ನೀವು ಚೆನ್ನಾಗಿದ್ದೀರಿ ಎಂದು ಸಂತೋಷ.")
a("im_ok","I AM OKAY","मैं ठीक हूँ","ನಾನು ಚೆನ್ನಾಗಿದ್ದೇನೆ")
a("metrics_line","Impact %1$.1f g · free-fall %2$d ms · rotation %3$d° · still %4$.1f s","झटका %1$.1f g · फ्री-फॉल %2$d ms · घुमाव %3$d° · स्थिर %4$.1f s","ಆಘಾತ %1$.1f g · ಫ್ರೀ-ಫಾಲ್ %2$d ms · ತಿರುಗುವಿಕೆ %3$d° · ನಿಶ್ಚಲ %4$.1f s")
a("pin_label","PIN to cancel","रद्द करने के लिए PIN","ರದ್ದುಮಾಡಲು PIN")
a("confirm","Confirm","पुष्टि करें","ದೃಢೀಕರಿಸಿ")
a("pin_wrong","Wrong PIN","गलत PIN","ತಪ್ಪು PIN")
a("incoming_call","Incoming call","आने वाली कॉल","ಒಳಬರುವ ಕರೆ")
a("app_locked","VERA Guard is locked","VERA Guard लॉक है","VERA Guard ಲಾಕ್ ಆಗಿದೆ")
a("unlock","Unlock","अनलॉक करें","ಅನ್‌ಲಾಕ್ ಮಾಡಿ")
a("quick_exit","Quick exit","तुरंत बाहर निकलें","ತಕ್ಷಣ ನಿರ್ಗಮಿಸಿ")
a("tab_home","Home","होम","ಮುಖಪುಟ")
a("tab_nearby","Nearby","आस-पास","ಹತ್ತಿರ")
a("tab_journey","Journey","यात्रा","ಪ್ರಯಾಣ")
a("tab_chat","Ask VERA","VERA से पूछें","VERA ಕೇಳಿ")
a("tab_profile","Profile","प्रोफ़ाइल","ಪ್ರೊಫೈಲ್")
a("perm_notifications","Notifications","सूचनाएँ","ಅಧಿಸೂಚನೆಗಳು")
a("perm_location","Location","लोकेशन","ಸ್ಥಳ")
a("perm_background_location","Location: allow all the time","लोकेशन: हमेशा अनुमति दें","ಸ್ಥಳ: ಯಾವಾಗಲೂ ಅನುಮತಿಸಿ")
a("perm_sms","Send SMS","SMS भेजें","SMS ಕಳುಹಿಸಿ")
a("perm_phone","Phone calls","फ़ोन कॉल","ಫೋನ್ ಕರೆಗಳು")
a("perm_microphone","Microphone","माइक्रोफ़ोन","ಮೈಕ್ರೋಫೋನ್")
a("perm_battery","Battery optimization off","बैटरी ऑप्टिमाइज़ेशन बंद","ಬ್ಯಾಟರಿ ಆಪ್ಟಿಮೈಸೇಶನ್ ಆಫ್")
a("perm_fullscreen","Full-screen alerts","फ़ुल-स्क्रीन अलर्ट","ಪೂರ್ಣ ಪರದೆ ಎಚ್ಚರಿಕೆಗಳು")
a("why_notifications","Needed to show the protection status and fall alerts.","सुरक्षा स्थिति और गिरने के अलर्ट दिखाने के लिए ज़रूरी।","ರಕ್ಷಣೆಯ ಸ್ಥಿತಿ ಮತ್ತು ಬೀಳುವಿಕೆ ಎಚ್ಚರಿಕೆಗಳನ್ನು ತೋರಿಸಲು ಅಗತ್ಯ.")
a("why_location","Needed to tell your contacts and responders where you are.","आपके संपर्कों और सहायकों को आपकी जगह बताने के लिए ज़रूरी।","ನಿಮ್ಮ ಸಂಪರ್ಕಗಳು ಮತ್ತು ರಕ್ಷಕರಿಗೆ ನೀವು ಎಲ್ಲಿದ್ದೀರಿ ಎಂದು ತಿಳಿಸಲು ಅಗತ್ಯ.")
a("why_background_location","Needed so location works when the screen is off.","स्क्रीन बंद होने पर भी लोकेशन के लिए ज़रूरी।","ಪರದೆ ಆಫ್ ಆಗಿದ್ದಾಗಲೂ ಸ್ಥಳ ಕೆಲಸ ಮಾಡಲು ಅಗತ್ಯ.")
a("why_sms","Needed to text your emergency contacts without internet.","इंटरनेट के बिना आपातकालीन संपर्कों को SMS भेजने के लिए ज़रूरी।","ಇಂಟರ್ನೆಟ್ ಇಲ್ಲದೆ ತುರ್ತು ಸಂಪರ್ಕಗಳಿಗೆ SMS ಕಳುಹಿಸಲು ಅಗತ್ಯ.")
a("why_phone","Needed to place the SOS call to your primary contact.","प्राथमिक संपर्क को SOS कॉल करने के लिए ज़रूरी।","ಪ್ರಾಥಮಿಕ ಸಂಪರ್ಕಕ್ಕೆ SOS ಕರೆ ಮಾಡಲು ಅಗತ್ಯ.")
a("why_microphone","Needed for the optional keyword trigger and audio evidence.","वैकल्पिक कीवर्ड ट्रिगर और ऑडियो सबूत के लिए ज़रूरी।","ಐಚ್ಛಿಕ ಕೀವರ್ಡ್ ಟ್ರಿಗರ್ ಮತ್ತು ಆಡಿಯೋ ಸಾಕ್ಷ್ಯಕ್ಕೆ ಅಗತ್ಯ.")
a("why_battery","Keeps protection alive in the background. Phone makers may also need extra steps.","बैकग्राउंड में सुरक्षा चालू रखता है। कुछ फ़ोन में अतिरिक्त कदम ज़रूरी हो सकते हैं।","ಹಿನ್ನೆಲೆಯಲ್ಲಿ ರಕ್ಷಣೆಯನ್ನು ಜೀವಂತವಾಗಿರಿಸುತ್ತದೆ. ಕೆಲವು ಫೋನ್‌ಗಳಲ್ಲಿ ಹೆಚ್ಚುವರಿ ಹಂತಗಳು ಬೇಕಾಗಬಹುದು.")
a("why_fullscreen","Lets the fall alert open over the lock screen.","गिरने का अलर्ट लॉक स्क्रीन पर खोलने देता है।","ಬೀಳುವಿಕೆ ಎಚ್ಚರಿಕೆಯನ್ನು ಲಾಕ್ ಸ್ಕ್ರೀನ್ ಮೇಲೆ ತೆರೆಯಲು ಅನುಮತಿಸುತ್ತದೆ.")
a("protection_active","Protection is ON","सुरक्षा चालू है","ರಕ್ಷಣೆ ಆನ್ ಆಗಿದೆ")
a("protection_incomplete","Protection is incomplete","सुरक्षा अधूरी है","ರಕ್ಷಣೆ ಅಪೂರ್ಣವಾಗಿದೆ")
a("protection_hint","Grant the items below so alerts work when the phone is locked.","फ़ोन लॉक होने पर भी अलर्ट काम करें, इसके लिए नीचे दी गई अनुमतियाँ दें।","ಫೋನ್ ಲಾಕ್ ಆಗಿದ್ದಾಗಲೂ ಎಚ್ಚರಿಕೆಗಳು ಕೆಲಸ ಮಾಡಲು ಕೆಳಗಿನ ಅನುಮತಿಗಳನ್ನು ನೀಡಿ.")
a("sos_active","SOS is active","SOS सक्रिय है","SOS ಸಕ್ರಿಯವಾಗಿದೆ")
a("stop_sos","Stop SOS","SOS बंद करें","SOS ನಿಲ್ಲಿಸಿ")
a("sos_button","SOS","SOS","SOS")
a("sos_hint","Press and hold for 2 seconds to send a silent SOS.","साइलेंट SOS भेजने के लिए 2 सेकंड दबाए रखें।","ಮೌನ SOS ಕಳುಹಿಸಲು 2 ಸೆಕೆಂಡ್ ಒತ್ತಿ ಹಿಡಿಯಿರಿ.")
a("setup_checklist","Setup checklist","सेटअप चेकलिस्ट","ಸೆಟಪ್ ಪರಿಶೀಲನಾ ಪಟ್ಟಿ")
a("grant","Grant","अनुमति दें","ಅನುಮತಿಸಿ")
a("tools","Tools","टूल","ಉಪಕರಣಗಳು")
a("fake_call","Fake call","नकली कॉल","ನಕಲಿ ಕರೆ")
a("start_fake_call","Start fake call","नकली कॉल शुरू करें","ನಕಲಿ ಕರೆ ಪ್ರಾರಂಭಿಸಿ")
a("demo_tools","Demo tools","डेमो टूल","ಡೆಮೊ ಉಪಕರಣಗಳು")
a("sim_severity","Simulated severity: %d","अनुकरण की गंभीरता: %d","ಅನುಕರಣೆಯ ತೀವ್ರತೆ: %d")
a("sim_fall","Simulate fall","गिरना सिम्युलेट करें","ಬೀಳುವಿಕೆ ಅನುಕರಿಸಿ")
a("sim_sos","Simulate silent SOS","साइलेंट SOS सिम्युलेट करें","ಮೌನ SOS ಅನುಕರಿಸಿ")
a("sim_journey_hint","Start a journey on the Journey tab, then simulate a deviation or a dead phone.","यात्रा टैब में यात्रा शुरू करें, फिर रास्ता भटकना या फ़ोन बंद होना सिम्युलेट करें।","ಪ್ರಯಾಣ ಟ್ಯಾಬ್‌ನಲ್ಲಿ ಪ್ರಯಾಣ ಪ್ರಾರಂಭಿಸಿ, ನಂತರ ಮಾರ್ಗ ತಪ್ಪುವಿಕೆ ಅಥವಾ ಫೋನ್ ಸ್ಥಗಿತ ಅನುಕರಿಸಿ.")
a("event_log","Event log","इवेंट लॉग","ಘಟನೆ ಲಾಗ್")
a("no_events","No events yet","अभी तक कोई इवेंट नहीं","ಇನ್ನೂ ಯಾವುದೇ ಘಟನೆಗಳಿಲ್ಲ")
a("disclaimer_emergency","VERA Guard assists but cannot guarantee help. In an emergency always call 112 or 108.","VERA Guard सहायता करता है पर मदद की गारंटी नहीं देता। आपातकाल में हमेशा 112 या 108 पर कॉल करें।","VERA Guard ಸಹಾಯ ಮಾಡುತ್ತದೆ ಆದರೆ ಸಹಾಯದ ಖಾತರಿ ನೀಡುವುದಿಲ್ಲ. ತುರ್ತು ಸಂದರ್ಭದಲ್ಲಿ ಯಾವಾಗಲೂ 112 ಅಥವಾ 108 ಗೆ ಕರೆ ಮಾಡಿ.")
a("filter_all","All","सभी","ಎಲ್ಲಾ")
a("hospitals","Hospitals","अस्पताल","ಆಸ್ಪತ್ರೆಗಳು")
a("clinics","Clinics","क्लिनिक","ಕ್ಲಿನಿಕ್‌ಗಳು")
a("pharmacies","Pharmacies","दवा की दुकानें","ಔಷಧಾಲಯಗಳು")
a("refresh","Refresh","ताज़ा करें","ರಿಫ್ರೆಶ್")
a("loading","Loading…","लोड हो रहा है…","ಲೋಡ್ ಆಗುತ್ತಿದೆ…")
a("offline_cached","Offline: showing saved results from %s ago","ऑफ़लाइन: %s पहले के सहेजे परिणाम दिखा रहे हैं","ಆಫ್‌ಲೈನ್: %s ಹಿಂದಿನ ಉಳಿಸಿದ ಫಲಿತಾಂಶಗಳನ್ನು ತೋರಿಸಲಾಗುತ್ತಿದೆ")
a("nearby_error","Could not load nearby places. Check your connection and location.","आस-पास की जगहें लोड नहीं हो सकीं। इंटरनेट और लोकेशन जाँचें।","ಹತ್ತಿರದ ಸ್ಥಳಗಳನ್ನು ಲೋಡ್ ಮಾಡಲಾಗಲಿಲ್ಲ. ಇಂಟರ್ನೆಟ್ ಮತ್ತು ಸ್ಥಳ ಪರಿಶೀಲಿಸಿ.")
a("you_are_here","You are here","आप यहाँ हैं","ನೀವು ಇಲ್ಲಿದ್ದೀರಿ")
a("nothing_found","Nothing found nearby","आस-पास कुछ नहीं मिला","ಹತ್ತಿರ ಏನೂ ಸಿಗಲಿಲ್ಲ")
a("open_now","Open now","अभी खुला है","ಈಗ ತೆರೆದಿದೆ")
a("closed","Closed","बंद","ಮುಚ್ಚಿದೆ")
a("call","Call","कॉल","ಕರೆ")
a("directions","Directions","रास्ता","ದಾರಿ")
a("eta_at","Arrive by %s","%s तक पहुँचना है","%s ರೊಳಗೆ ತಲುಪಬೇಕು")
a("share_link","Share tracking link","ट्रैकिंग लिंक साझा करें","ಟ್ರ್ಯಾಕಿಂಗ್ ಲಿಂಕ್ ಹಂಚಿಕೊಳ್ಳಿ")
a("extend_eta","+15 min","+15 मिनट","+15 ನಿಮಿಷ")
a("end_journey","End journey","यात्रा समाप्त करें","ಪ್ರಯಾಣ ಮುಗಿಸಿ")
a("sim_deviation","Simulate route deviation","रास्ता भटकना सिम्युलेट करें","ಮಾರ್ಗ ತಪ್ಪುವಿಕೆ ಅನುಕರಿಸಿ")
a("sim_dead","Simulate phone dying","फ़ोन बंद होना सिम्युलेट करें","ಫೋನ್ ಸ್ಥಗಿತ ಅನುಕರಿಸಿ")
a("sim_dead_hint","Stops location updates; the server alerts followers after the watchdog time.","लोकेशन अपडेट रोक देता है; वॉचडॉग समय के बाद सर्वर फ़ॉलोअर्स को अलर्ट करता है।","ಸ್ಥಳ ಅಪ್‌ಡೇಟ್‌ಗಳನ್ನು ನಿಲ್ಲಿಸುತ್ತದೆ; ವಾಚ್‌ಡಾಗ್ ಸಮಯದ ನಂತರ ಸರ್ವರ್ ಅನುಸರಿಸುವವರಿಗೆ ಎಚ್ಚರಿಕೆ ನೀಡುತ್ತದೆ.")
a("journey_error","Could not start the journey on the server. Check your connection.","सर्वर पर यात्रा शुरू नहीं हो सकी। इंटरनेट जाँचें।","ಸರ್ವರ್‌ನಲ್ಲಿ ಪ್ರಯಾಣ ಪ್ರಾರಂಭಿಸಲಾಗಲಿಲ್ಲ. ಇಂಟರ್ನೆಟ್ ಪರಿಶೀಲಿಸಿ.")
a("destination","Destination","गंतव्य","ಗಮ್ಯಸ್ಥಾನ")
a("search_place","Search a place","जगह खोजें","ಸ್ಥಳ ಹುಡುಕಿ")
a("search","Search","खोजें","ಹುಡುಕಿ")
a("search_failed","Search failed. Check your connection.","खोज विफल रही। इंटरनेट जाँचें।","ಹುಡುಕಾಟ ವಿಫಲವಾಯಿತು. ಇಂಟರ್ನೆಟ್ ಪರಿಶೀಲಿಸಿ.")
a("mode","Travel mode","यात्रा का साधन","ಪ್ರಯಾಣದ ವಿಧ")
a("mode_walking","Walking","पैदल","ನಡಿಗೆ")
a("mode_cab","Cab / auto","कैब / ऑटो","ಕ್ಯಾಬ್ / ಆಟೋ")
a("mode_bike","Bike","बाइक","ಬೈಕ್")
a("mode_car","Car","कार","ಕಾರು")
a("buffer_minutes","Delay buffer (minutes)","देरी का बफ़र (मिनट)","ವಿಳಂಬ ಬಫರ್ (ನಿಮಿಷ)")
a("followers","Who follows this journey","इस यात्रा को कौन देखेगा","ಈ ಪ್ರಯಾಣವನ್ನು ಯಾರು ಅನುಸರಿಸುತ್ತಾರೆ")
a("no_contacts","No contacts yet. Add them in Profile.","अभी कोई संपर्क नहीं। प्रोफ़ाइल में जोड़ें।","ಇನ್ನೂ ಸಂಪರ್ಕಗಳಿಲ್ಲ. ಪ್ರೊಫೈಲ್‌ನಲ್ಲಿ ಸೇರಿಸಿ.")
a("cab_details","Cab details (optional)","कैब का विवरण (वैकल्पिक)","ಕ್ಯಾಬ್ ವಿವರ (ಐಚ್ಛಿಕ)")
a("vehicle_number","Vehicle number","वाहन नंबर","ವಾಹನ ಸಂಖ್ಯೆ")
a("driver_name","Driver name","ड्राइवर का नाम","ಚಾಲಕರ ಹೆಸರು")
a("pick_follower","Choose at least one follower","कम से कम एक फ़ॉलोअर चुनें","ಕನಿಷ್ಠ ಒಬ್ಬ ಅನುಸರಿಸುವವರನ್ನು ಆಯ್ಕೆಮಾಡಿ")
a("no_location","Could not get your location. Turn on GPS and try again.","आपकी लोकेशन नहीं मिल सकी। GPS चालू करके फिर कोशिश करें।","ನಿಮ್ಮ ಸ್ಥಳ ಸಿಗಲಿಲ್ಲ. GPS ಆನ್ ಮಾಡಿ ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ.")
a("start_journey","Start journey","यात्रा शुरू करें","ಪ್ರಯಾಣ ಪ್ರಾರಂಭಿಸಿ")
a("st_on_route","On route","रास्ते पर","ಮಾರ್ಗದಲ್ಲಿದೆ")
a("st_deviated","Off route","रास्ते से भटके","ಮಾರ್ಗ ತಪ್ಪಿದೆ")
a("st_stopped","Stopped for too long","बहुत देर से रुके हैं","ತುಂಬಾ ಹೊತ್ತು ನಿಂತಿದೆ")
a("st_delayed","Delayed","देरी","ವಿಳಂಬ")
a("st_arrived","Arrived","पहुँच गए","ತಲುಪಿದೆ")
a("st_alert","Alert sent","अलर्ट भेजा गया","ಎಚ್ಚರಿಕೆ ಕಳುಹಿಸಲಾಗಿದೆ")
a("chat_title","Ask VERA","VERA से पूछें","VERA ಕೇಳಿ")
a("chat_disclaimer","First-aid information only, not a diagnosis. In an emergency call 112 or 108.","केवल प्राथमिक उपचार की जानकारी, निदान नहीं। आपातकाल में 112 या 108 पर कॉल करें।","ಪ್ರಥಮ ಚಿಕಿತ್ಸೆಯ ಮಾಹಿತಿ ಮಾತ್ರ, ರೋಗನಿರ್ಣಯವಲ್ಲ. ತುರ್ತು ಸಂದರ್ಭದಲ್ಲಿ 112 ಅಥವಾ 108 ಗೆ ಕರೆ ಮಾಡಿ.")
a("chat_personalise","Use my shared profile fields in answers","उत्तरों में मेरी साझा प्रोफ़ाइल जानकारी इस्तेमाल करें","ಉತ್ತರಗಳಲ್ಲಿ ನನ್ನ ಹಂಚಿದ ಪ್ರೊಫೈಲ್ ಮಾಹಿತಿ ಬಳಸಿ")
a("chat_hint","Type your question","अपना सवाल लिखें","ನಿಮ್ಮ ಪ್ರಶ್ನೆ ಬರೆಯಿರಿ")
a("chat_empty","Ask about first aid or safety. Works offline for common emergencies.","प्राथमिक उपचार या सुरक्षा के बारे में पूछें। आम आपात स्थितियों के लिए ऑफ़लाइन भी चलता है।","ಪ್ರಥಮ ಚಿಕಿತ್ಸೆ ಅಥವಾ ಸುರಕ್ಷತೆ ಬಗ್ಗೆ ಕೇಳಿ. ಸಾಮಾನ್ಯ ತುರ್ತು ಸ್ಥಿತಿಗಳಿಗೆ ಆಫ್‌ಲೈನ್‌ನಲ್ಲಿಯೂ ಕೆಲಸ ಮಾಡುತ್ತದೆ.")
a("offline_cards","Offline cards: %s","ऑफ़लाइन कार्ड: %s","ಆಫ್‌ಲೈನ್ ಕಾರ್ಡ್‌ಗಳು: %s")
a("offline_answer","Offline answer","ऑफ़लाइन उत्तर","ಆಫ್‌ಲೈನ್ ಉತ್ತರ")
a("speak","Read aloud","पढ़कर सुनाएँ","ಓದಿ ಹೇಳಿ")
a("send","Send","भेजें","ಕಳುಹಿಸಿ")
a("chip_fell","I fell down","मैं गिर गया/गई","ನಾನು ಬಿದ್ದೆ")
a("chip_bleeding","Heavy bleeding","भारी रक्तस्राव","ತೀವ್ರ ರಕ್ತಸ್ರಾವ")
a("chip_cpr","How to do CPR","CPR कैसे करें","CPR ಹೇಗೆ ಮಾಡುವುದು")
a("chip_hospital","Nearest hospital","नज़दीकी अस्पताल","ಹತ್ತಿರದ ಆಸ್ಪತ್ರೆ")
a("chip_journey","Start a journey","यात्रा शुरू करें","ಪ್ರಯಾಣ ಪ್ರಾರಂಭಿಸಿ")
a("chip_call","Call my contact","मेरे संपर्क को कॉल करें","ನನ್ನ ಸಂಪರ್ಕಕ್ಕೆ ಕರೆ ಮಾಡಿ")

# ---- redesign: sensors, women's safety, helplines
a("hero_tagline","Silent safety, always on","चुपचाप सुरक्षा, हमेशा चालू","ಮೌನ ಸುರಕ್ಷತೆ, ಸದಾ ಆನ್")
a("pill_protected","PROTECTED","सुरक्षित","ರಕ್ಷಿತ")
a("pill_setup","SETUP NEEDED","सेटअप ज़रूरी","ಸೆಟಪ್ ಬೇಕು")
a("checklist_progress","%1$d of %2$d ready","%1$d / %2$d तैयार","%1$d / %2$d ಸಿದ್ಧ")
a("show","Show","दिखाएँ","ತೋರಿಸಿ")
a("hide","Hide","छिपाएँ","ಮರೆಮಾಡಿ")
a("close","Close","बंद करें","ಮುಚ್ಚಿ")
a("sos_hold","Hold","दबाए रखें","ಒತ್ತಿ ಹಿಡಿಯಿರಿ")
a("sensor_title","Motion sensors","मोशन सेंसर","ಮೋಷನ್ ಸೆನ್ಸರ್‌ಗಳು")
a("sensor_live","LIVE","लाइव","ಲೈವ್")
a("sensor_off","NOT RUNNING","बंद है","ನಡೆಯುತ್ತಿಲ್ಲ")
a("sensor_start","Start","शुरू करें","ಪ್ರಾರಂಭಿಸಿ")
a("sensor_none","This phone has no accelerometer, so fall and shake detection cannot work here.","इस फ़ोन में एक्सेलेरोमीटर नहीं है, इसलिए गिरना और शेक पहचान यहाँ काम नहीं करेगी।","ಈ ಫೋನ್‌ನಲ್ಲಿ ಆಕ್ಸೆಲೆರೋಮೀಟರ್ ಇಲ್ಲ, ಆದ್ದರಿಂದ ಬೀಳುವಿಕೆ ಮತ್ತು ಶೇಕ್ ಪತ್ತೆ ಇಲ್ಲಿ ಕೆಲಸ ಮಾಡುವುದಿಲ್ಲ.")
a("sensor_nogyro","No gyroscope found. Detection still works using the accelerometer.","जाइरोस्कोप नहीं मिला। एक्सेलेरोमीटर से पहचान फिर भी काम करती है।","ಗೈರೋಸ್ಕೋಪ್ ಇಲ್ಲ. ಆಕ್ಸೆಲೆರೋಮೀಟರ್‌ನಿಂದ ಪತ್ತೆ ಇನ್ನೂ ಕೆಲಸ ಮಾಡುತ್ತದೆ.")
a("sensor_force","Force (g)","बल (g)","ಬಲ (g)")
a("sensor_rotation","Rotation (°/s)","घूर्णन (°/s)","ತಿರುಗುವಿಕೆ (°/s)")
a("sensor_state","Detector","डिटेक्टर","ಡಿಟೆಕ್ಟರ್")
a("sensor_hint","Move or tilt your phone and watch the graph react. A real shake sends a real SOS, so use Demo mode in Settings to test safely.","फ़ोन को हिलाएँ या झुकाएँ और ग्राफ़ को बदलते देखें। असली शेक असली SOS भेजता है, इसलिए सुरक्षित जाँच के लिए सेटिंग्स में डेमो मोड इस्तेमाल करें।","ಫೋನ್ ಅಲುಗಿಸಿ ಅಥವಾ ಓರೆಯಾಗಿಸಿ ಮತ್ತು ಗ್ರಾಫ್ ಬದಲಾಗುವುದನ್ನು ನೋಡಿ. ನಿಜವಾದ ಶೇಕ್ ನಿಜವಾದ SOS ಕಳುಹಿಸುತ್ತದೆ, ಆದ್ದರಿಂದ ಸುರಕ್ಷಿತ ಪರೀಕ್ಷೆಗೆ ಸೆಟ್ಟಿಂಗ್ಸ್‌ನಲ್ಲಿ ಡೆಮೊ ಮೋಡ್ ಬಳಸಿ.")
a("det_idle","Watching","निगरानी जारी","ಗಮನಿಸುತ್ತಿದೆ")
a("det_freefall","Free-fall","मुक्त पतन","ಮುಕ್ತ ಪತನ")
a("det_impact","Impact","टक्कर","ಡಿಕ್ಕಿ")
a("det_still","Checking stillness","स्थिरता जाँच","ಸ್ಥಿರತೆ ಪರಿಶೀಲನೆ")
a("det_confirm","Confirming","पुष्टि हो रही है","ದೃಢೀಕರಿಸುತ್ತಿದೆ")
a("det_alert","Alerting","अलर्ट भेज रहा है","ಎಚ್ಚರಿಕೆ ಕಳುಹಿಸುತ್ತಿದೆ")
a("ws_title","Women's safety","महिला सुरक्षा","ಮಹಿಳಾ ಸುರಕ್ಷತೆ")
a("ws_sub","Quick tools for when you feel unsafe. Tap any tile.","जब आप असुरक्षित महसूस करें तो काम आने वाले त्वरित साधन। किसी भी टाइल को दबाएँ।","ನೀವು ಅಸುರಕ್ಷಿತ ಎನಿಸಿದಾಗ ಬೇಗನೆ ಬಳಸುವ ಸಾಧನಗಳು. ಯಾವುದೇ ಟೈಲ್ ಒತ್ತಿ.")
a("ws_siren","Loud siren","तेज़ सायरन","ಜೋರಾದ ಸೈರನ್")
a("ws_siren_stop","Stop siren","सायरन बंद करें","ಸೈರನ್ ನಿಲ್ಲಿಸಿ")
a("ws_share","Share location","लोकेशन भेजें","ಸ್ಥಳ ಹಂಚಿ")
a("ws_share_msg","I want you to know where I am right now: %s","मैं अभी कहाँ हूँ, यह आपको बताना चाहती/चाहता हूँ: %s","ನಾನು ಈಗ ಎಲ್ಲಿದ್ದೇನೆ ಎಂದು ನಿಮಗೆ ತಿಳಿಸಲು ಬಯಸುತ್ತೇನೆ: %s")
a("ws_safe_walk","Safe walk","सुरक्षित यात्रा","ಸುರಕ್ಷಿತ ನಡಿಗೆ")
a("ws_nearby","Nearby help","पास में मदद","ಹತ್ತಿರದ ಸಹಾಯ")
a("ws_helplines","Helplines","हेल्पलाइन","ಸಹಾಯವಾಣಿಗಳು")
a("ws_shake_on","Shake-to-SOS is ON","शेक-टू-SOS चालू है","ಶೇಕ್-ಟು-SOS ಆನ್ ಆಗಿದೆ")
a("ws_shake_off","Shake-to-SOS is OFF","शेक-टू-SOS बंद है","ಶೇಕ್-ಟು-SOS ಆಫ್ ಆಗಿದೆ")
a("fc_ring_in","Ring in","कॉल आएगी","ಕರೆ ಬರುವುದು")
a("fc_hint","The screen will look like an incoming call from %s.","स्क्रीन पर %s की आती हुई कॉल जैसा दिखेगा।","ಪರದೆಯು %s ಅವರಿಂದ ಬರುವ ಕರೆಯಂತೆ ಕಾಣಿಸುತ್ತದೆ.")
a("hl_112","National emergency number","राष्ट्रीय आपातकालीन नंबर","ರಾಷ್ಟ್ರೀಯ ತುರ್ತು ಸಂಖ್ಯೆ")
a("hl_181","Women Helpline","महिला हेल्पलाइन","ಮಹಿಳಾ ಸಹಾಯವಾಣಿ")
a("hl_1091","Women in distress","संकट में महिलाएँ","ಸಂಕಷ್ಟದಲ್ಲಿರುವ ಮಹಿಳೆಯರು")
a("hl_100","Police","पुलिस","ಪೊಲೀಸ್")
a("hl_108","Ambulance","एम्बुलेंस","ಆಂಬ್ಯುಲೆನ್ಸ್")
a("hl_1098","Childline","चाइल्डलाइन","ಚೈಲ್ಡ್‌ಲೈನ್")
a("hl_1930","Cyber crime","साइबर अपराध","ಸೈಬರ್ ಅಪರಾಧ")
for k,en,hi,kn in [
 ("sec_profile","Profile","प्रोफ़ाइल","ಪ್ರೊಫೈಲ್"),("sec_contacts","Contacts","संपर्क","ಸಂಪರ್ಕಗಳು"),("sec_sharing","Sharing","साझा करना","ಹಂಚಿಕೆ"),
 ("sec_security","Security","सुरक्षा","ಭದ್ರತೆ"),("sec_settings","Settings","सेटिंग्स","ಸೆಟ್ಟಿಂಗ್‌ಗಳು"),
 ("pick_photo","Choose photo","फ़ोटो चुनें","ಫೋಟೋ ಆಯ್ಕೆಮಾಡಿ"),("remove","Remove","हटाएँ","ತೆಗೆದುಹಾಕಿ"),("full_name","Full name","पूरा नाम","ಪೂರ್ಣ ಹೆಸರು"),
 ("dob","Date of birth (YYYY-MM-DD)","जन्म तिथि (YYYY-MM-DD)","ಹುಟ್ಟಿದ ದಿನಾಂಕ (YYYY-MM-DD)"),("age","Age","आयु","ವಯಸ್ಸು"),("gender","Gender","लिंग","ಲಿಂಗ"),
 ("g_female","Female","महिला","ಮಹಿಳೆ"),("g_male","Male","पुरुष","ಪುರುಷ"),("g_other","Other","अन्य","ಇತರೆ"),
 ("imperial_units","Use feet / pounds","फ़ीट / पाउंड का उपयोग करें","ಅಡಿ / ಪೌಂಡ್ ಬಳಸಿ"),("imperial_hint","Stored internally as cm and kg","आंतरिक रूप से सेमी और किग्रा में सहेजा जाता है","ಆಂತರಿಕವಾಗಿ ಸೆಂ.ಮೀ ಮತ್ತು ಕೆ.ಜಿ ಯಲ್ಲಿ ಉಳಿಸಲಾಗುತ್ತದೆ"),
 ("height_cm","Height (cm)","ऊँचाई (सेमी)","ಎತ್ತರ (ಸೆಂ.ಮೀ)"),("height_ftin","Height (ft.in)","ऊँचाई (फ़ीट.इंच)","ಎತ್ತರ (ಅಡಿ.ಇಂಚು)"),
 ("weight_kg","Weight (kg)","वज़न (किग्रा)","ತೂಕ (ಕೆ.ಜಿ)"),("weight_lb","Weight (lb)","वज़न (पाउंड)","ತೂಕ (ಪೌಂಡ್)"),
 ("blood_group","Blood group","रक्त समूह","ರಕ್ತದ ಗುಂಪು"),("allergies","Allergies (comma separated)","एलर्जी (अल्पविराम से अलग करें)","ಅಲರ್ಜಿಗಳು (ಅಲ್ಪವಿರಾಮದಿಂದ ಬೇರ್ಪಡಿಸಿ)"),
 ("conditions","Medical conditions","चिकित्सीय स्थितियाँ","ವೈದ್ಯಕೀಯ ಸ್ಥಿತಿಗಳು"),("medications","Medications","दवाइयाँ","ಔಷಧಿಗಳು"),
 ("doctor_name","Doctor name","डॉक्टर का नाम","ವೈದ್ಯರ ಹೆಸರು"),("doctor_phone","Doctor phone","डॉक्टर का फ़ोन","ವೈದ್ಯರ ಫೋನ್"),
 ("home_address","Home address","घर का पता","ಮನೆ ವಿಳಾಸ"),("ice_note","Emergency note","आपातकालीन नोट","ತುರ್ತು ಟಿಪ್ಪಣಿ"),
 ("profile_hint","Only fields you allow under Sharing are ever sent to contacts or responders.","केवल वही जानकारी संपर्कों या सहायकों को जाती है जिसे आपने साझा करने की अनुमति दी है।","ಹಂಚಿಕೆಯಲ್ಲಿ ನೀವು ಅನುಮತಿಸಿದ ಮಾಹಿತಿ ಮಾತ್ರ ಸಂಪರ್ಕಗಳು ಅಥವಾ ರಕ್ಷಕರಿಗೆ ಹೋಗುತ್ತದೆ."),
 ("save","Save","सहेजें","ಉಳಿಸಿ"),("saved","Saved","सहेज लिया गया","ಉಳಿಸಲಾಗಿದೆ"),("cancel","Cancel","रद्द करें","ರದ್ದುಮಾಡಿ"),("edit","Edit","बदलें","ಸಂಪಾದಿಸಿ"),
 ("err_name","Enter a name of 2 to 60 characters","2 से 60 अक्षरों का नाम लिखें","2 ರಿಂದ 60 ಅಕ್ಷರಗಳ ಹೆಸರು ನಮೂದಿಸಿ"),
 ("err_dob","Use the format YYYY-MM-DD, not in the future","YYYY-MM-DD प्रारूप का उपयोग करें, भविष्य की तिथि नहीं","YYYY-MM-DD ರೂಪ ಬಳಸಿ, ಭವಿಷ್ಯದ ದಿನಾಂಕ ಬೇಡ"),
 ("err_height","Height must be between 50 and 250 cm","ऊँचाई 50 से 250 सेमी के बीच होनी चाहिए","ಎತ್ತರ 50 ರಿಂದ 250 ಸೆಂ.ಮೀ ನಡುವೆ ಇರಬೇಕು"),
 ("err_weight","Weight must be between 2 and 400 kg","वज़न 2 से 400 किग्रा के बीच होना चाहिए","ತೂಕ 2 ರಿಂದ 400 ಕೆ.ಜಿ ನಡುವೆ ಇರಬೇಕು"),
 ("err_phone","Enter a valid phone number (7 to 15 digits, optional +)","सही फ़ोन नंबर लिखें (7 से 15 अंक, वैकल्पिक +)","ಸರಿಯಾದ ಫೋನ್ ಸಂಖ್ಯೆ ನಮೂದಿಸಿ (7 ರಿಂದ 15 ಅಂಕೆ, ಐಚ್ಛಿಕ +)"),
 ("err_pin_same","The cancel PIN and the duress PIN must be different","रद्द PIN और दबाव PIN अलग होने चाहिए","ರದ್ದು PIN ಮತ್ತು ಒತ್ತಡ PIN ಬೇರೆಬೇರೆ ಆಗಿರಬೇಕು"),
 ("contacts_hint","Up to 5 contacts. The starred one is called first.","अधिकतम 5 संपर्क। तारे वाले को पहले कॉल किया जाता है।","ಗರಿಷ್ಠ 5 ಸಂಪರ್ಕಗಳು. ನಕ್ಷತ್ರ ಇರುವವರಿಗೆ ಮೊದಲು ಕರೆ ಮಾಡಲಾಗುತ್ತದೆ."),
 ("add_contact","Add contact","संपर्क जोड़ें","ಸಂಪರ್ಕ ಸೇರಿಸಿ"),("max_contacts","You can add up to 5 contacts","आप अधिकतम 5 संपर्क जोड़ सकते हैं","ನೀವು ಗರಿಷ್ಠ 5 ಸಂಪರ್ಕಗಳನ್ನು ಸೇರಿಸಬಹುದು"),
 ("contact","Contact","संपर्क","ಸಂಪರ್ಕ"),("phone","Phone","फ़ोन","ಫೋನ್"),("relationship","Relationship","रिश्ता","ಸಂಬಂಧ"),
 ("sms_language","Language for their SMS","उनके SMS की भाषा","ಅವರ SMS ಭಾಷೆ"),("primary_contact","Primary contact (called first)","प्राथमिक संपर्क (पहले कॉल)","ಪ್ರಾಥಮಿಕ ಸಂಪರ್ಕ (ಮೊದಲು ಕರೆ)"),
 ("sharing_hint","Choose what your contacts and responders can see during an alert. Nothing is shared unless an alert or journey is active.","चुनें कि अलर्ट के दौरान आपके संपर्क और सहायक क्या देख सकते हैं। अलर्ट या यात्रा सक्रिय न हो तो कुछ भी साझा नहीं होता।","ಎಚ್ಚರಿಕೆಯ ಸಮಯದಲ್ಲಿ ನಿಮ್ಮ ಸಂಪರ್ಕಗಳು ಮತ್ತು ರಕ್ಷಕರು ಏನನ್ನು ನೋಡಬಹುದು ಎಂದು ಆಯ್ಕೆಮಾಡಿ. ಎಚ್ಚರಿಕೆ ಅಥವಾ ಪ್ರಯಾಣ ಸಕ್ರಿಯವಾಗಿಲ್ಲದಿದ್ದರೆ ಏನೂ ಹಂಚಿಕೆಯಾಗುವುದಿಲ್ಲ."),
 ("sharing_preview","Preview: what responders will see","पूर्वावलोकन: सहायक क्या देखेंगे","ಮುನ್ನೋಟ: ರಕ್ಷಕರು ಏನು ನೋಡುತ್ತಾರೆ"),
 ("nothing_shared","Nothing will be shared","कुछ भी साझा नहीं होगा","ಏನೂ ಹಂಚಿಕೆಯಾಗುವುದಿಲ್ಲ"),
 ("sharing_note","Location is always included during an active alert.","सक्रिय अलर्ट के दौरान लोकेशन हमेशा शामिल रहती है।","ಸಕ್ರಿಯ ಎಚ್ಚರಿಕೆಯ ಸಮಯದಲ್ಲಿ ಸ್ಥಳ ಯಾವಾಗಲೂ ಸೇರಿರುತ್ತದೆ."),
 ("pin_hint","The cancel PIN stops a false alarm. Without a PIN anyone holding the phone can cancel.","रद्द PIN झूठे अलार्म को रोकता है। PIN के बिना फ़ोन पकड़ने वाला कोई भी रद्द कर सकता है।","ರದ್ದು PIN ಸುಳ್ಳು ಎಚ್ಚರಿಕೆಯನ್ನು ನಿಲ್ಲಿಸುತ್ತದೆ. PIN ಇಲ್ಲದಿದ್ದರೆ ಫೋನ್ ಹಿಡಿದವರು ಯಾರಾದರೂ ರದ್ದುಮಾಡಬಹುದು."),
 ("cancel_pin","Cancel PIN (4 to 8 digits)","रद्द PIN (4 से 8 अंक)","ರದ್ದು PIN (4 ರಿಂದ 8 ಅಂಕೆ)"),("cancel_pin_set","Cancel PIN is set. Enter a new one to change","रद्द PIN सेट है। बदलने के लिए नया लिखें","ರದ್ದು PIN ಹೊಂದಿಸಲಾಗಿದೆ. ಬದಲಿಸಲು ಹೊಸದನ್ನು ನಮೂದಿಸಿ"),
 ("duress_pin","Duress PIN (4 to 8 digits)","दबाव PIN (4 से 8 अंक)","ಒತ್ತಡ PIN (4 ರಿಂದ 8 ಅಂಕೆ)"),("duress_pin_set","Duress PIN is set. Enter a new one to change","दबाव PIN सेट है। बदलने के लिए नया लिखें","ಒತ್ತಡ PIN ಹೊಂದಿಸಲಾಗಿದೆ. ಬದಲಿಸಲು ಹೊಸದನ್ನು ನಮೂದಿಸಿ"),
 ("duress_hint","If someone forces you to cancel, enter this PIN. The screen looks cancelled but the alert continues silently.","कोई आपको रद्द करने को मजबूर करे तो यह PIN डालें। स्क्रीन पर रद्द दिखेगा पर अलर्ट चुपचाप जारी रहेगा।","ಯಾರಾದರೂ ರದ್ದುಮಾಡಲು ಒತ್ತಾಯಿಸಿದರೆ ಈ PIN ನಮೂದಿಸಿ. ಪರದೆಯಲ್ಲಿ ರದ್ದಾದಂತೆ ಕಾಣುತ್ತದೆ ಆದರೆ ಎಚ್ಚರಿಕೆ ಮೌನವಾಗಿ ಮುಂದುವರಿಯುತ್ತದೆ."),
 ("app_lock","Lock the app with fingerprint or screen lock","फ़िंगरप्रिंट या स्क्रीन लॉक से ऐप लॉक करें","ಫಿಂಗರ್‌ಪ್ರಿಂಟ್ ಅಥವಾ ಸ್ಕ್ರೀನ್ ಲಾಕ್‌ನಿಂದ ಆ್ಯಪ್ ಲಾಕ್ ಮಾಡಿ"),
 ("app_lock_hint","SOS triggers and fall alerts still work while the app is locked.","ऐप लॉक होने पर भी SOS ट्रिगर और गिरने के अलर्ट काम करते हैं।","ಆ್ಯಪ್ ಲಾಕ್ ಆಗಿದ್ದರೂ SOS ಟ್ರಿಗರ್ ಮತ್ತು ಬೀಳುವಿಕೆ ಎಚ್ಚರಿಕೆಗಳು ಕೆಲಸ ಮಾಡುತ್ತವೆ."),
 ("your_data","Your data","आपका डेटा","ನಿಮ್ಮ ಡೇಟಾ"),
 ("data_hint","Everything is stored encrypted on this phone. Deleting also removes your sessions and journeys from the server.","सब कुछ इस फ़ोन में एन्क्रिप्टेड रूप में सहेजा जाता है। हटाने पर सर्वर से आपके सत्र और यात्राएँ भी हट जाती हैं।","ಎಲ್ಲವನ್ನೂ ಈ ಫೋನ್‌ನಲ್ಲಿ ಎನ್‌ಕ್ರಿಪ್ಟ್ ಮಾಡಿ ಉಳಿಸಲಾಗುತ್ತದೆ. ಅಳಿಸಿದರೆ ಸರ್ವರ್‌ನಿಂದ ನಿಮ್ಮ ಸೆಷನ್‌ಗಳು ಮತ್ತು ಪ್ರಯಾಣಗಳೂ ತೆಗೆಯಲ್ಪಡುತ್ತವೆ."),
 ("export_data","Export my data","मेरा डेटा निर्यात करें","ನನ್ನ ಡೇಟಾ ರಫ್ತು ಮಾಡಿ"),("delete_all","Delete all my data","मेरा सारा डेटा हटाएँ","ನನ್ನ ಎಲ್ಲಾ ಡೇಟಾ ಅಳಿಸಿ"),
 ("delete_confirm","This permanently deletes your profile, contacts, history and server records. It cannot be undone.","इससे आपकी प्रोफ़ाइल, संपर्क, इतिहास और सर्वर रिकॉर्ड हमेशा के लिए हट जाएँगे। इसे वापस नहीं किया जा सकता।","ಇದು ನಿಮ್ಮ ಪ್ರೊಫೈಲ್, ಸಂಪರ್ಕಗಳು, ಇತಿಹಾಸ ಮತ್ತು ಸರ್ವರ್ ದಾಖಲೆಗಳನ್ನು ಶಾಶ್ವತವಾಗಿ ಅಳಿಸುತ್ತದೆ. ಇದನ್ನು ಹಿಂತಿರುಗಿಸಲಾಗುವುದಿಲ್ಲ."),
 ("set_general","General","सामान्य","ಸಾಮಾನ್ಯ"),("language","Language","भाषा","ಭಾಷೆ"),("theme","Theme","थीम","ಥೀಮ್"),
 ("theme_system","System","सिस्टम","ಸಿಸ್ಟಮ್"),("theme_light","Light","हल्का","ಬೆಳಕು"),("theme_dark","Dark","गहरा","ಗಾಢ"),
 ("server_url","Server URL","सर्वर URL","ಸರ್ವರ್ URL"),("emergency_number","Emergency number","आपातकालीन नंबर","ತುರ್ತು ಸಂಖ್ಯೆ"),
 ("emergency_hint","Used for the optional ambulance call after an unanswered fall alert. Never dialled in demo mode.","बिना जवाब वाले गिरने के अलर्ट के बाद वैकल्पिक एम्बुलेंस कॉल के लिए। डेमो मोड में कभी डायल नहीं होता।","ಉತ್ತರಿಸದ ಬೀಳುವಿಕೆ ಎಚ್ಚರಿಕೆಯ ನಂತರ ಐಚ್ಛಿಕ ಆಂಬ್ಯುಲೆನ್ಸ್ ಕರೆಗೆ ಬಳಸಲಾಗುತ್ತದೆ. ಡೆಮೊ ಮೋಡ್‌ನಲ್ಲಿ ಎಂದಿಗೂ ಡಯಲ್ ಆಗುವುದಿಲ್ಲ."),
 ("set_detection","Fall detection","गिरने की पहचान","ಬೀಳುವಿಕೆ ಪತ್ತೆ"),("set_fall","Fall and crash detection","गिरने और दुर्घटना की पहचान","ಬೀಳುವಿಕೆ ಮತ್ತು ಅಪಘಾತ ಪತ್ತೆ"),
 ("set_sensitivity","Sensitivity","संवेदनशीलता","ಸಂವೇದನಾಶೀಲತೆ"),("sensitivity_hint","Higher catches more falls but may cause more false alarms.","अधिक मान ज़्यादा गिरना पकड़ता है पर झूठे अलार्म भी बढ़ सकते हैं।","ಹೆಚ್ಚಿನ ಮೌಲ್ಯ ಹೆಚ್ಚು ಬೀಳುವಿಕೆ ಹಿಡಿಯುತ್ತದೆ ಆದರೆ ಸುಳ್ಳು ಎಚ್ಚರಿಕೆಗಳೂ ಹೆಚ್ಚಬಹುದು."),
 ("cd_low","Countdown low (s)","काउंटडाउन कम (से.)","ಕೌಂಟ್‌ಡೌನ್ ಕಡಿಮೆ (ಸೆ.)"),("cd_medium","Countdown medium (s)","काउंटडाउन मध्यम (से.)","ಕೌಂಟ್‌ಡೌನ್ ಮಧ್ಯಮ (ಸೆ.)"),("cd_high","Countdown high (s)","काउंटडाउन अधिक (से.)","ಕೌಂಟ್‌ಡೌನ್ ಹೆಚ್ಚು (ಸೆ.)"),
 ("checkin_seconds","Journey check-in wait (seconds)","यात्रा चेक-इन प्रतीक्षा (सेकंड)","ಪ್ರಯಾಣ ಚೆಕ್-ಇನ್ ಕಾಯುವಿಕೆ (ಸೆಕೆಂಡ್)"),
 ("checkin_silent","Silent check-in","साइलेंट चेक-इन","ಮೌನ ಚೆಕ್-ಇನ್"),("checkin_silent_hint","Vibrate only, no sound or voice.","केवल कंपन, कोई आवाज़ नहीं।","ಕಂಪನ ಮಾತ್ರ, ಧ್ವನಿ ಇಲ್ಲ."),
 ("set_silent","Silent SOS and privacy","साइलेंट SOS और गोपनीयता","ಮೌನ SOS ಮತ್ತು ಗೋಪ್ಯತೆ"),
 ("set_shake","Shake to send SOS","SOS के लिए फ़ोन हिलाएँ","SOS ಗಾಗಿ ಫೋನ್ ಅಲುಗಾಡಿಸಿ"),("set_shake_hint","About 4 hard shakes within 3 seconds.","3 सेकंड में लगभग 4 ज़ोरदार झटके।","3 ಸೆಕೆಂಡ್‌ನಲ್ಲಿ ಸುಮಾರು 4 ಜೋರಾದ ಅಲುಗಾಟ."),
 ("set_widget","Home screen widget","होम स्क्रीन विजेट","ಹೋಮ್ ಸ್ಕ್ರೀನ್ ವಿಜೆಟ್"),("set_tile","Quick settings tile","क्विक सेटिंग्स टाइल","ಕ್ವಿಕ್ ಸೆಟ್ಟಿಂಗ್ಸ್ ಟೈಲ್"),
 ("set_notif_sos","SOS button in the notification","सूचना में SOS बटन","ಅಧಿಸೂಚನೆಯಲ್ಲಿ SOS ಬಟನ್"),
 ("set_keyword","Voice keyword (experimental)","आवाज़ कीवर्ड (प्रायोगिक)","ಧ್ವನಿ ಕೀವರ್ಡ್ (ಪ್ರಾಯೋಗಿಕ)"),("set_keyword_hint","Listens for help me, बचाओ or ಸಹಾಯ. Uses more battery.","help me, बचाओ या ಸಹಾಯ सुनता है। ज़्यादा बैटरी लेता है।","help me, बचाओ ಅಥವಾ ಸಹಾಯ ಕೇಳುತ್ತದೆ. ಹೆಚ್ಚು ಬ್ಯಾಟರಿ ಬಳಸುತ್ತದೆ."),
 ("set_audio","Record audio during SOS","SOS के दौरान ऑडियो रिकॉर्ड करें","SOS ಸಮಯದಲ್ಲಿ ಆಡಿಯೋ ರೆಕಾರ್ಡ್ ಮಾಡಿ"),("set_audio_hint","Short clips are uploaded to the server. Android may block background recording on some phones.","छोटी क्लिप सर्वर पर अपलोड होती हैं। कुछ फ़ोन में Android बैकग्राउंड रिकॉर्डिंग रोक सकता है।","ಸಣ್ಣ ಕ್ಲಿಪ್‌ಗಳನ್ನು ಸರ್ವರ್‌ಗೆ ಅಪ್‌ಲೋಡ್ ಮಾಡಲಾಗುತ್ತದೆ. ಕೆಲವು ಫೋನ್‌ಗಳಲ್ಲಿ Android ಹಿನ್ನೆಲೆ ರೆಕಾರ್ಡಿಂಗ್ ತಡೆಯಬಹುದು."),
 ("quick_exit_setting","Show a quick-exit button","तुरंत-बाहर बटन दिखाएँ","ತಕ್ಷಣ-ನಿರ್ಗಮನ ಬಟನ್ ತೋರಿಸಿ"),("fake_call_name","Fake call caller name","नकली कॉल करने वाले का नाम","ನಕಲಿ ಕರೆ ಮಾಡುವವರ ಹೆಸರು"),
 ("set_journey","Safe journey","सुरक्षित यात्रा","ಸುರಕ್ಷಿತ ಪ್ರಯಾಣ"),("watchdog_seconds","Silence alert after (seconds)","चुप्पी पर अलर्ट (सेकंड बाद)","ಮೌನ ಎಚ್ಚರಿಕೆ (ಸೆಕೆಂಡ್ ನಂತರ)"),
 ("watchdog_hint","If the server hears nothing from your phone for this long, your followers are alerted.","यदि सर्वर को इतनी देर आपके फ़ोन से कुछ न मिले तो आपके फ़ॉलोअर्स को अलर्ट किया जाता है।","ಇಷ್ಟು ಹೊತ್ತು ನಿಮ್ಮ ಫೋನ್‌ನಿಂದ ಸರ್ವರ್‌ಗೆ ಏನೂ ಸಿಗದಿದ್ದರೆ ನಿಮ್ಮ ಅನುಸರಿಸುವವರಿಗೆ ಎಚ್ಚರಿಕೆ ನೀಡಲಾಗುತ್ತದೆ."),
 ("set_demo","Demo mode","डेमो मोड","ಡೆಮೊ ಮೋಡ್"),("demo_mode","Demo mode","डेमो मोड","ಡೆಮೊ ಮೋಡ್"),("demo_phone","Demo phone number","डेमो फ़ोन नंबर","ಡೆಮೊ ಫೋನ್ ಸಂಖ್ಯೆ"),
 ("set_reliability","Keep protection running","सुरक्षा चालू रखें","ರಕ್ಷಣೆ ಚಾಲನೆಯಲ್ಲಿಡಿ"),
 ("battery_hint","Phone makers often stop background apps. Turn off battery optimization and allow autostart.","फ़ोन कंपनियाँ अक्सर बैकग्राउंड ऐप बंद कर देती हैं। बैटरी ऑप्टिमाइज़ेशन बंद करें और ऑटोस्टार्ट की अनुमति दें।","ಫೋನ್ ತಯಾರಕರು ಹೆಚ್ಚಾಗಿ ಹಿನ್ನೆಲೆ ಆ್ಯಪ್‌ಗಳನ್ನು ನಿಲ್ಲಿಸುತ್ತಾರೆ. ಬ್ಯಾಟರಿ ಆಪ್ಟಿಮೈಸೇಶನ್ ಆಫ್ ಮಾಡಿ ಮತ್ತು ಆಟೋಸ್ಟಾರ್ಟ್ ಅನುಮತಿಸಿ."),
 ("battery_ok","Battery optimization is off for VERA Guard","VERA Guard के लिए बैटरी ऑप्टिमाइज़ेशन बंद है","VERA Guard ಗೆ ಬ್ಯಾಟರಿ ಆಪ್ಟಿಮೈಸೇಶನ್ ಆಫ್ ಆಗಿದೆ"),
 ("battery_not_ok","Battery optimization may stop VERA Guard","बैटरी ऑप्टिमाइज़ेशन VERA Guard को रोक सकता है","ಬ್ಯಾಟರಿ ಆಪ್ಟಿಮೈಸೇಶನ್ VERA Guard ಅನ್ನು ನಿಲ್ಲಿಸಬಹುದು"),
 ("battery_open","Allow background use","बैकग्राउंड उपयोग की अनुमति दें","ಹಿನ್ನೆಲೆ ಬಳಕೆ ಅನುಮತಿಸಿ"),("app_settings","Open app settings","ऐप सेटिंग्स खोलें","ಆ್ಯಪ್ ಸೆಟ್ಟಿಂಗ್‌ಗಳನ್ನು ತೆರೆಯಿರಿ"),
 ("battery_oem","Xiaomi: Autostart on, Battery saver No restrictions. Samsung: Never sleeping apps. OnePlus and Realme: allow auto-launch and background activity.","Xiaomi: ऑटोस्टार्ट चालू, बैटरी सेवर में No restrictions। Samsung: Never sleeping apps। OnePlus और Realme: ऑटो-लॉन्च और बैकग्राउंड गतिविधि की अनुमति।","Xiaomi: ಆಟೋಸ್ಟಾರ್ಟ್ ಆನ್, ಬ್ಯಾಟರಿ ಸೇವರ್‌ನಲ್ಲಿ No restrictions. Samsung: Never sleeping apps. OnePlus ಮತ್ತು Realme: ಆಟೋ-ಲಾಂಚ್ ಮತ್ತು ಹಿನ್ನೆಲೆ ಚಟುವಟಿಕೆ ಅನುಮತಿಸಿ."),
]: a(k,en,hi,kn)

keys = set(l.strip() for l in open("/tmp/keys.txt") if l.strip()) if pathlib.Path("/tmp/keys.txt").exists() else set()
src = ""
for p in list((ROOT/"java").rglob("*.kt")) + list((ROOT/"res").rglob("*.xml")) + [ROOT/"AndroidManifest.xml"]:
    if "/values" in str(p): continue
    src += p.read_text()
used = set(re.findall(r"R\.string\.([a-z0-9_]+)", src)) | set(re.findall(r"@string/([a-z0-9_]+)", src))
missing = sorted(used - set(S)); unused = sorted(set(S) - used)
if missing: print("MISSING translations:", missing); sys.exit(1)
if unused: print("note: unused keys (kept):", unused)

def fmt(s):
    s = escape(s).replace("'", "\\'").replace('"', '\\"')
    if s.startswith(("@", "?")): s = "\\" + s
    return s
for i, d in enumerate(["values", "values-hi", "values-kn"]):
    out = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    if d == "values": out.append('    <string name="translation_note" translatable="false">en/hi/kn generated by tools/gen_strings.py</string>')
    for k in sorted(S): out.append(f'    <string name="{k}">{fmt(S[k][i])}</string>')
    out.append("</resources>")
    (ROOT/"res"/d).mkdir(parents=True, exist_ok=True)
    (ROOT/"res"/d/"strings.xml").write_text("\n".join(out) + "\n")
print(f"OK: {len(S)} keys x 3 languages; {len(used)} used")
