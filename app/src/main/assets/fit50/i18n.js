(function(){
  'use strict';

  const SUPPORTED = [
    {code:'auto', name:'Auto / Device language', rtl:false},
    {code:'en', name:'English', rtl:false},
    {code:'he', name:'עברית', rtl:true},
    {code:'ar', name:'العربية', rtl:true},
    {code:'fr', name:'Français', rtl:false},
    {code:'es', name:'Español', rtl:false},
    {code:'de', name:'Deutsch', rtl:false},
    {code:'it', name:'Italiano', rtl:false},
    {code:'pt', name:'Português', rtl:false},
    {code:'ru', name:'Русский', rtl:false},
    {code:'tr', name:'Türkçe', rtl:false},
    {code:'zh', name:'中文', rtl:false},
    {code:'ja', name:'日本語', rtl:false},
    {code:'ko', name:'한국어', rtl:false},
    {code:'hi', name:'हिन्दी', rtl:false},
    {code:'pl', name:'Polski', rtl:false},
    {code:'nl', name:'Nederlands', rtl:false}
  ];

  const SUPPORTED_CODES = new Set(SUPPORTED.filter(x=>x.code!=='auto').map(x=>x.code));
  const RTL = new Set(['he','ar']);
  const HEBREW_RE = /[\u0590-\u05FF]/;

  const EN = {
    'בית':'Home',
    'מתיחות':'Stretch',
    'התקדמות':'Progress',
    'עוד':'More',
    'הגדרות':'Settings',
    'התחברות':'Sign in',
    'המשך עם Google':'Continue with Google',
    'או':'or',
    'אימייל':'Email',
    'כתובת אימייל':'Email address',
    'סיסמה':'Password',
    'שכחת סיסמה?':'Forgot password?',
    'שכחתי סיסמה':'Forgot password',
    'אין חשבון?':'No account?',
    'הירשם':'Sign up',
    'הרשמה':'Sign up',
    'צור חשבון':'Create account',
    'שם מלא':'Full name',
    'אימות סיסמה':'Confirm password',
    'ברוך הבא':'Welcome',
    'שלח קישור לאיפוס':'Send reset link',
    'חזרה להתחברות':'Back to sign in',
    'התחל אימון':'Start workout',
    'המשך אימון':'Resume workout',
    'אימון מושהה':'Paused workout',
    'האימון של היום':'Today’s workout',
    'האימון המותאם שלך':'Your personalized workout',
    'השבוע שלך':'Your week',
    'תוכנית מלאה':'Full plan',
    'המסע שלך':'Your journey',
    'עוד פעילויות':'More activities',
    'שבוע':'Week',
    'אימונים שהושלמו':'Workouts completed',
    'רצף ימים':'Day streak',
    'דקות':'Minutes',
    'דקות תנועה':'Movement minutes',
    'רצף נוכחי':'Current streak',
    'הצעד הבא:':'Next step:',
    'חדש':'New',
    'פרופיל אישי':'Personal profile',
    'חשבון':'Account',
    'התראות וחוויה':'Notifications & experience',
    'תזכורות אימון':'Workout reminders',
    'רטט עדין':'Gentle haptics',
    'צלילי טיימר':'Timer sounds',
    'שפה':'Language',
    'בריאות ובטיחות':'Health & safety',
    'שאלון בטיחות':'Safety questionnaire',
    'הנחיות בטיחות':'Safety guidelines',
    'משפטי ואודות':'Legal & about',
    'מדיניות פרטיות':'Privacy policy',
    'תנאי שימוש':'Terms of use',
    'צור קשר':'Contact us',
    'דרג את האפליקציה':'Rate the app',
    'עזרה ותמיכה':'Help & support',
    'התנתקות':'Sign out',
    'מחיקת חשבון':'Delete account',
    'עריכת פרופיל':'Edit profile',
    'שינוי סיסמה':'Change password',
    'שינוי אימייל':'Change email',
    'שמור':'Save',
    'שמור שינויים':'Save changes',
    'ביטול':'Cancel',
    'סגור':'Close',
    'כן':'Yes',
    'לא':'No',
    'אימון':'Workout',
    'תרגיל':'Exercise',
    'תרגילים':'Exercises',
    'סט':'Set',
    'סטים':'Sets',
    'חזרות':'Reps',
    'שניות':'Seconds',
    'שנ׳':'sec',
    'מנוחה':'Rest',
    'מנוחה קצרה':'Short rest',
    'סיים סט':'Complete set',
    'השהה אימון':'Pause workout',
    'בטל אימון':'Cancel workout',
    'איך לבצע':'How to do it',
    'מינון באימון':'Workout dose',
    'דגש חשוב':'Important cue',
    'חימום':'Warm-up',
    'קירור':'Cool-down',
    'קירור ומתיחות':'Cool-down & stretching',
    'שאלון בריאות':'Health questionnaire',
    'המשך':'Continue',
    'חזרה':'Back',
    'סיים שאלון':'Finish questionnaire',
    'שמור תשובות':'Save answers',
    'שלב':'Step',
    'גיל':'Age',
    'שנים':'years',
    'מין':'Gender',
    'גבר':'Man',
    'אישה':'Woman',
    'אחר / מעדיף לא לציין':'Other / prefer not to say',
    'רמת פעילות':'Activity level',
    'מצב רפואי':'Medical conditions',
    'רמת כאב':'Pain level',
    'מטרה ראשית':'Main goal',
    'תדירות אימונים':'Workout frequency',
    'משך אימון':'Workout duration',
    'בוקר':'Morning',
    'צהריים':'Afternoon',
    'ערב':'Evening',
    'בוקר טוב':'Good morning',
    'צהריים טובים':'Good afternoon',
    'ערב טוב':'Good evening',
    'היום':'Today',
    'הושלם':'Completed',
    'יום מנוחה':'Rest day',
    'תן לגוף להתאושש':'Let your body recover',
    'תוכנית החודש':'Monthly plan',
    'שיתוף':'Share',
    'שתף את ההתקדמות':'Share progress',
    'שתף עם':'Share with',
    'נשמר':'Saved',
    'הועתק':'Copied',
    'אינסטגרם':'Instagram',
    'פייסבוק':'Facebook',
    'סטורי':'Story',
    'שיאים אישיים':'Personal bests',
    'אין עדיין היסטוריה':'No history yet',
    'עוד אין נתונים להציג':'No data to show yet',
    'התחל רצף':'Start sequence',
    'סיים רצף':'Finish sequence',
    'המשך מתיחות':'Resume stretching',
    'נשימה':'Breathing',
    'שאף':'Inhale',
    'נשוף':'Exhale',
    'פתיחת בוקר':'Morning opener',
    'שחרור ערב':'Evening release',
    'רגיעה':'Relaxation',
    'עוצמה':'Intensity',
    'משך':'Duration',
    'מצב':'Status',
    'אוטומטי — שפת המכשיר':'Automatic — device language',
    'שפת המכשיר':'Device language',
    'טוען תרגום…':'Loading translation…',
    'שליחה':'Send',
    'שלח':'Send',
    'הצג':'Show',
    'הסתר':'Hide',
    'מתחבר...':'Signing in…',
    'שולח...':'Sending…',
    'יוצר חשבון...':'Creating account…',
    'נסה שוב':'Try again',
    'נסה לשמור שוב':'Try saving again',
    'החשבון נמחק':'Account deleted',
    'התנתקת בהצלחה':'Signed out successfully',
    'שמור סיסמה חדשה':'Save new password',
    'סיסמה חדשה':'New password',
    'סיסמה נוכחית':'Current password',
    'אימות סיסמה חדשה':'Confirm new password',
    'כתובת אימייל חדשה':'New email address',
    'תזכורות':'Reminders',
    'צלילים':'Sounds',
    'רטט':'Haptics'
  };

  const QUICK = {
    ar:{
      'בית':'الرئيسية','מתיחות':'تمدد','התקדמות':'التقدم','עוד':'المزيد','הגדרות':'الإعدادات',
      'התחברות':'تسجيل الدخول','אימייל':'البريد الإلكتروني','סיסמה':'كلمة المرور','שפה':'اللغة',
      'התחל אימון':'ابدأ التمرين','המשך אימון':'استئناف التمرين','התנתקות':'تسجيل الخروج',
      'מחיקת חשבון':'حذف الحساب','המשך':'متابعة','חזרה':'رجوع','שמור':'حفظ'
    },
    fr:{
      'בית':'Accueil','מתיחות':'Étirements','התקדמות':'Progression','עוד':'Plus','הגדרות':'Paramètres',
      'התחברות':'Connexion','אימייל':'E-mail','סיסמה':'Mot de passe','שפה':'Langue',
      'התחל אימון':'Commencer','המשך אימון':'Reprendre','התנתקות':'Déconnexion',
      'מחיקת חשבון':'Supprimer le compte','המשך':'Continuer','חזרה':'Retour','שמור':'Enregistrer'
    },
    es:{
      'בית':'Inicio','מתיחות':'Estiramientos','התקדמות':'Progreso','עוד':'Más','הגדרות':'Ajustes',
      'התחברות':'Iniciar sesión','אימייל':'Correo','סיסמה':'Contraseña','שפה':'Idioma',
      'התחל אימון':'Empezar entrenamiento','המשך אימון':'Reanudar entrenamiento','התנתקות':'Cerrar sesión',
      'מחיקת חשבון':'Eliminar cuenta','המשך':'Continuar','חזרה':'Volver','שמור':'Guardar'
    },
    de:{
      'בית':'Start','מתיחות':'Dehnen','התקדמות':'Fortschritt','עוד':'Mehr','הגדרות':'Einstellungen',
      'התחברות':'Anmelden','אימייל':'E-Mail','סיסמה':'Passwort','שפה':'Sprache',
      'התחל אימון':'Training starten','המשך אימון':'Training fortsetzen','התנתקות':'Abmelden',
      'מחיקת חשבון':'Konto löschen','המשך':'Weiter','חזרה':'Zurück','שמור':'Speichern'
    },
    it:{
      'בית':'Home','מתיחות':'Stretching','התקדמות':'Progressi','עוד':'Altro','הגדרות':'Impostazioni',
      'התחברות':'Accedi','אימייל':'Email','סיסמה':'Password','שפה':'Lingua',
      'התחל אימון':'Inizia allenamento','המשך אימון':'Riprendi allenamento','התנתקות':'Esci',
      'מחיקת חשבון':'Elimina account','המשך':'Continua','חזרה':'Indietro','שמור':'Salva'
    },
    pt:{
      'בית':'Início','מתיחות':'Alongamento','התקדמות':'Progresso','עוד':'Mais','הגדרות':'Configurações',
      'התחברות':'Entrar','אימייל':'E-mail','סיסמה':'Senha','שפה':'Idioma',
      'התחל אימון':'Iniciar treino','המשך אימון':'Retomar treino','התנתקות':'Sair',
      'מחיקת חשבון':'Excluir conta','המשך':'Continuar','חזרה':'Voltar','שמור':'Salvar'
    },
    ru:{
      'בית':'Главная','מתיחות':'Растяжка','התקדמות':'Прогресс','עוד':'Ещё','הגדרות':'Настройки',
      'התחברות':'Войти','אימייל':'Эл. почта','סיסמה':'Пароль','שפה':'Язык',
      'התחל אימון':'Начать тренировку','המשך אימון':'Продолжить тренировку','התנתקות':'Выйти',
      'מחיקת חשבון':'Удалить аккаунт','המשך':'Продолжить','חזרה':'Назад','שמור':'Сохранить'
    },
    tr:{
      'בית':'Ana Sayfa','מתיחות':'Esneme','התקדמות':'İlerleme','עוד':'Daha Fazla','הגדרות':'Ayarlar',
      'התחברות':'Giriş yap','אימייל':'E-posta','סיסמה':'Şifre','שפה':'Dil',
      'התחל אימון':'Antrenmanı başlat','המשך אימון':'Antrenmana devam et','התנתקות':'Çıkış yap',
      'מחיקת חשבון':'Hesabı sil','המשך':'Devam','חזרה':'Geri','שמור':'Kaydet'
    },
    zh:{
      'בית':'主页','מתיחות':'拉伸','התקדמות':'进度','עוד':'更多','הגדרות':'设置',
      'התחברות':'登录','אימייל':'电子邮件','סיסמה':'密码','שפה':'语言',
      'התחל אימון':'开始训练','המשך אימון':'继续训练','התנתקות':'退出登录',
      'מחיקת חשבון':'删除账户','המשך':'继续','חזרה':'返回','שמור':'保存'
    },
    ja:{
      'בית':'ホーム','מתיחות':'ストレッチ','התקדמות':'進捗','עוד':'その他','הגדרות':'設定',
      'התחברות':'ログイン','אימייל':'メール','סיסמה':'パスワード','שפה':'言語',
      'התחל אימון':'ワークアウト開始','המשך אימון':'ワークアウト再開','התנתקות':'ログアウト',
      'מחיקת חשבון':'アカウント削除','המשך':'続ける','חזרה':'戻る','שמור':'保存'
    },
    ko:{
      'בית':'홈','מתיחות':'스트레칭','התקדמות':'진행 상황','עוד':'더보기','הגדרות':'설정',
      'התחברות':'로그인','אימייל':'이메일','סיסמה':'비밀번호','שפה':'언어',
      'התחל אימון':'운동 시작','המשך אימון':'운동 계속','התנתקות':'로그아웃',
      'מחיקת חשבון':'계정 삭제','המשך':'계속','חזרה':'뒤로','שמור':'저장'
    },
    hi:{
      'בית':'होम','מתיחות':'स्ट्रेचिंग','התקדמות':'प्रगति','עוד':'और','הגדרות':'सेटिंग्स',
      'התחברות':'साइन इन','אימייל':'ईमेल','סיסמה':'पासवर्ड','שפה':'भाषा',
      'התחל אימון':'वर्कआउट शुरू करें','המשך אימון':'वर्कआउट जारी रखें','התנתקות':'साइन आउट',
      'מחיקת חשבון':'खाता हटाएँ','המשך':'जारी रखें','חזרה':'वापस','שמור':'सहेजें'
    },
    pl:{
      'בית':'Start','מתיחות':'Rozciąganie','התקדמות':'Postęp','עוד':'Więcej','הגדרות':'Ustawienia',
      'התחברות':'Zaloguj się','אימייל':'E-mail','סיסמה':'Hasło','שפה':'Język',
      'התחל אימון':'Rozpocznij trening','המשך אימון':'Wznów trening','התנתקות':'Wyloguj',
      'מחיקת חשבון':'Usuń konto','המשך':'Dalej','חזרה':'Wstecz','שמור':'Zapisz'
    },
    nl:{
      'בית':'Home','מתיחות':'Rekken','התקדמות':'Voortgang','עוד':'Meer','הגדרות':'Instellingen',
      'התחברות':'Inloggen','אימייל':'E-mail','סיסמה':'Wachtwoord','שפה':'Taal',
      'התחל אימון':'Training starten','המשך אימון':'Training hervatten','התנתקות':'Uitloggen',
      'מחיקת חשבון':'Account verwijderen','המשך':'Doorgaan','חזרה':'Terug','שמור':'Opslaan'
    }
  };

  function normalize(code){
    const raw=(code||'').toLowerCase();
    let base=raw.split(/[-_]/)[0];
    if(base==='iw') base='he';
    return SUPPORTED_CODES.has(base) ? base : 'en';
  }

  function nativeCall(name, fallback){
    try{
      if(window.Fit50Native && typeof window.Fit50Native[name]==='function'){
        const value=window.Fit50Native[name]();
        if(value!==undefined && value!==null && value!=='') return value;
      }
    }catch(e){}
    return fallback;
  }

  const preferred=nativeCall('getPreferredLanguage','auto');
  const device=nativeCall('getDeviceLanguage',navigator.language||'en');
  let current=preferred==='auto' ? normalize(device) : normalize(preferred);

  function packFor(lang){
    if(lang==='he') return {};
    if(lang==='en') return EN;
    return Object.assign({},EN,QUICK[lang]||{});
  }

  function translateCore(value){
    const trimmed=String(value||'').trim();
    if(!trimmed || current==='he') return null;
    const pack=packFor(current);
    return pack[trimmed] || null;
  }

  function replacePreservingSpace(original,replacement){
    const s=String(original);
    const leading=(s.match(/^\s*/)||[''])[0];
    const trailing=(s.match(/\s*$/)||[''])[0];
    return leading+replacement+trailing;
  }

  function applyDirection(){
    const rtl=RTL.has(current);
    document.documentElement.lang=current;
    document.documentElement.dir=rtl?'rtl':'ltr';

    let style=document.getElementById('fit50-i18n-direction');
    if(!style){
      style=document.createElement('style');
      style.id='fit50-i18n-direction';
      (document.head||document.documentElement).appendChild(style);
    }

    style.textContent =
      'html[dir="ltr"] body{direction:ltr!important;text-align:left}' +
      'html[dir="ltr"] input,html[dir="ltr"] textarea,html[dir="ltr"] select{direction:ltr!important;text-align:left!important}' +
      'html[dir="ltr"] .list-label,html[dir="ltr"] .section-title,html[dir="ltr"] .form-title,html[dir="ltr"] label{text-align:left}' +
      'html[dir="ltr"] .list-arrow{transform:scaleX(-1)}' +
      'html[dir="rtl"] body{direction:rtl!important}';
  }

  const pendingTargets=new Map();
  const waiting=new Set();
  let requestTimer=null;
  let observer=null;

  function elementVisible(el){
    if(!el || el.nodeType!==1) return true;
    if(el.closest && el.closest('[data-no-i18n]')) return false;
    const tag=el.tagName;
    if(tag==='SCRIPT'||tag==='STYLE'||tag==='NOSCRIPT'||tag==='CODE'||tag==='PRE') return false;
    const style=window.getComputedStyle ? getComputedStyle(el) : null;
    if(style && (style.display==='none'||style.visibility==='hidden')) return false;
    return true;
  }

  function rememberTarget(original,target){
    if(!pendingTargets.has(original)) pendingTargets.set(original,[]);
    pendingTargets.get(original).push(target);
  }

  function queueMachine(original,target){
    if(current==='he'||!HEBREW_RE.test(original)) return;
    rememberTarget(original,target);
    if(waiting.has(original)) return;
    waiting.add(original);
    clearTimeout(requestTimer);
    requestTimer=setTimeout(flushMachine,80);
  }

  function translateTextNode(node){
    if(!node || !node.parentElement || !elementVisible(node.parentElement)) return;
    const raw=node.nodeValue||'';
    const trimmed=raw.trim();
    if(!trimmed || !HEBREW_RE.test(trimmed)) return;

    const core=translateCore(trimmed);
    if(core){
      node.nodeValue=replacePreservingSpace(raw,core);
      return;
    }

    queueMachine(trimmed,{type:'text',node:node,original:trimmed});
  }

  function translateAttribute(el,attr){
    if(!el || !elementVisible(el)) return;
    const raw=el.getAttribute(attr);
    if(!raw || !HEBREW_RE.test(raw)) return;
    const trimmed=raw.trim();

    const core=translateCore(trimmed);
    if(core){
      el.setAttribute(attr,core);
      return;
    }

    queueMachine(trimmed,{type:'attr',node:el,attr:attr,original:trimmed});
  }

  function scan(root){
    if(current==='he'||!root) return;

    if(root.nodeType===3){
      translateTextNode(root);
      return;
    }

    if(root.nodeType!==1 && root.nodeType!==9 && root.nodeType!==11) return;

    const base=root.nodeType===1 ? root : (root.body||root.documentElement);
    if(!base) return;

    if(base.nodeType===1){
      ['placeholder','title','aria-label'].forEach(a=>translateAttribute(base,a));
    }

    const walker=document.createTreeWalker(
      base,
      NodeFilter.SHOW_TEXT,
      {
        acceptNode:function(node){
          if(!node.parentElement||!elementVisible(node.parentElement)) return NodeFilter.FILTER_REJECT;
          return HEBREW_RE.test(node.nodeValue||'') ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
        }
      }
    );

    let node;
    while((node=walker.nextNode())) translateTextNode(node);

    if(base.querySelectorAll){
      base.querySelectorAll('[placeholder],[title],[aria-label]').forEach(el=>{
        ['placeholder','title','aria-label'].forEach(a=>translateAttribute(el,a));
      });
    }
  }

  function flushMachine(){
    if(current==='he'||waiting.size===0) return;
    const batch=Array.from(waiting).slice(0,120);
    batch.forEach(x=>waiting.delete(x));

    try{
      if(window.Fit50Native && typeof window.Fit50Native.translateTexts==='function'){
        window.Fit50Native.translateTexts(current,JSON.stringify(batch));
      }else{
        batch.forEach(x=>waiting.delete(x));
      }
    }catch(e){}

    if(waiting.size){
      clearTimeout(requestTimer);
      requestTimer=setTimeout(flushMachine,250);
    }
  }

  window.fit50TranslationResult=function(ok,language,json){
    if(normalize(language)!==current) return;

    let map={};
    try{map=JSON.parse(json||'{}')}catch(e){}

    Object.keys(map).forEach(original=>{
      const translated=String(map[original]||'').trim();
      if(!translated || translated===original) return;

      const targets=pendingTargets.get(original)||[];
      targets.forEach(t=>{
        try{
          if(t.type==='text' && t.node && t.node.isConnected){
            const raw=t.node.nodeValue||'';
            if(HEBREW_RE.test(raw)) t.node.nodeValue=replacePreservingSpace(raw,translated);
          }else if(t.type==='attr' && t.node && t.node.isConnected){
            const raw=t.node.getAttribute(t.attr)||'';
            if(HEBREW_RE.test(raw)) t.node.setAttribute(t.attr,translated);
          }
        }catch(e){}
      });
      pendingTargets.delete(original);
    });

    if(waiting.size){
      clearTimeout(requestTimer);
      requestTimer=setTimeout(flushMachine,100);
    }
  };

  function installObserver(){
    if(observer) observer.disconnect();
    observer=new MutationObserver(mutations=>{
      mutations.forEach(m=>{
        if(m.type==='childList'){
          m.addedNodes.forEach(scan);
        }else if(m.type==='attributes'){
          scan(m.target);
        }
      });
    });
    observer.observe(document.documentElement,{
      childList:true,
      subtree:true,
      attributes:true,
      attributeFilter:['class','style','placeholder','title','aria-label']
    });
  }

  function getPreferred(){
    return nativeCall('getPreferredLanguage',preferred||'auto');
  }

  function setLanguage(code){
    const value=code==='auto'?'auto':normalize(code);
    try{
      if(window.Fit50Native && typeof window.Fit50Native.setPreferredLanguage==='function'){
        window.Fit50Native.setPreferredLanguage(value);
      }
    }catch(e){}
    setTimeout(()=>location.reload(),80);
  }

  function currentLanguageName(){
    const item=SUPPORTED.find(x=>x.code===current);
    return item ? item.name : 'English';
  }

  function updateSettingsLanguageLabel(){
    const row=document.querySelector('[data-action="language"] .list-label small');
    if(!row) return;
    const pref=getPreferred();
    if(pref==='auto'){
      row.textContent=(current==='he'?'אוטומטי · ':'Auto · ')+currentLanguageName();
    }else{
      row.textContent=currentLanguageName();
    }
  }

  function openLanguagePicker(){
    if(typeof window.openSheet!=='function') return;

    const selected=getPreferred();
    const autoLabel=current==='he'?'אוטומטי — שפת המכשיר':'Automatic — device language';

    const html =
      '<div class="list" style="margin-bottom:20px">' +
      SUPPORTED.map(lang=>{
        const label=lang.code==='auto'?autoLabel:lang.name;
        const on=selected===lang.code;
        return '<button class="list-row" data-fit50-lang="'+lang.code+'" style="cursor:pointer">' +
          '<span class="list-label" dir="'+(lang.rtl?'rtl':'ltr')+'">'+label+'</span>' +
          (on?'<span style="font-size:20px;color:var(--forest)">✓</span>':'') +
        '</button>';
      }).join('') +
      '</div>';

    window.openSheet(current==='he'?'שפה':(translateCore('שפה')||'Language'),html);

    document.querySelectorAll('[data-fit50-lang]').forEach(btn=>{
      btn.onclick=function(){
        setLanguage(btn.getAttribute('data-fit50-lang'));
      };
    });
  }

  function boot(){
    applyDirection();
    if(current!=='he'){
      scan(document);
      installObserver();
    }
    updateSettingsLanguageLabel();
  }

  window.Fit50I18n={
    languages:SUPPORTED.slice(),
    getLanguage:()=>current,
    getPreferredLanguage:getPreferred,
    getLanguageName:currentLanguageName,
    setLanguage:setLanguage,
    openLanguagePicker:openLanguagePicker,
    translate:function(text){
      if(current==='he') return text;
      return translateCore(text)||text;
    },
    rescan:function(){scan(document);}
  };

  applyDirection();

  if(document.readyState==='loading'){
    document.addEventListener('DOMContentLoaded',boot,{once:true});
  }else{
    boot();
  }
})();
