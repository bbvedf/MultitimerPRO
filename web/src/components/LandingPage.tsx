import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Timer, Zap, Shield, Globe, ChevronRight, Play, BarChart3,
  BookOpen, Sun, Moon, ArrowUp, ArrowDown, User, HelpCircle,
  ChevronDown, Quote
} from 'lucide-react';
import { translations } from '../lib/translations';

type Language = 'en' | 'es' | 'fr' | 'de' | 'it' | 'pt';
type Theme = 'dark' | 'light';

const FeatureCard = ({ icon: Icon, title, description, color }: { icon: any, title: string, description: string, color: string }) => (
  <motion.div 
    whileHover={{ y: -5 }}
    className="bg-[var(--surface-dark)] p-8 rounded-3xl border border-[var(--border-color)] relative overflow-hidden group"
  >
    <div className="absolute top-0 left-0 w-full h-1 opacity-20 group-hover:opacity-100 transition-opacity" style={{ backgroundColor: color }} />
    <div className="w-12 h-12 rounded-2xl flex items-center justify-center mb-6" style={{ backgroundColor: `${color}10` }}>
      <Icon size={24} style={{ color: color }} />
    </div>
    <h3 className="font-headline text-xl font-bold mb-3 uppercase tracking-tight text-[var(--text-color)]">{title}</h3>
    <p className="text-[var(--on-surface-variant)] text-sm leading-relaxed">{description}</p>
  </motion.div>
);

const ShowcaseItem = ({ title, img, color, theme }: { title: string, img: string, color: string, theme: 'dark' | 'light' }) => (
  <div className="space-y-4">
    <div className="aspect-[9/16] rounded-3xl bg-[var(--surface-dark)] border border-[var(--border-color)] overflow-hidden relative group shadow-2xl">
      <motion.img 
        key={theme}
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        src={img} 
        alt={title} 
        className="w-full h-full object-cover transition-all duration-700 opacity-80 group-hover:opacity-100"
        onError={(e) => {
          (e.target as HTMLImageElement).src = `https://picsum.photos/seed/${title}-${theme}/600/1000`;
        }}
        referrerPolicy="no-referrer"
      />
      <div className="absolute inset-x-0 bottom-0 bg-[var(--surface-dark)] border-t border-[var(--border-color)] p-6">
        <div className="flex items-center gap-4">
          <div className="h-2 w-10 rounded-full" style={{ backgroundColor: color, boxShadow: `0 0 15px ${color}` }} />
          <p className="font-headline font-black text-sm uppercase tracking-[0.2em] text-[var(--text-color)]">{title}</p>
        </div>
      </div>
    </div>
  </div>
);

const FAQItem = ({ question, answer }: { question: string, answer: string }) => {
  const [isOpen, setIsOpen] = useState(false);
  return (
    <div className="border-b border-[var(--border-color)] last:border-0">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="w-full py-6 flex justify-between items-center text-left hover:text-neon-blue transition-colors group"
      >
        <span className="font-headline font-bold text-lg text-[var(--text-color)] group-hover:text-neon-blue">{question}</span>
        <ChevronDown size={20} className={`text-[var(--on-surface-variant)] transition-transform duration-300 ${isOpen ? 'rotate-180 text-neon-blue' : ''}`} />
      </button>
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="overflow-hidden"
          >
            <p className="pb-6 text-[var(--on-surface-variant)] leading-relaxed text-sm">
              {answer}
            </p>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

const LanguageSelector = ({ lang, setLang, flags }: { lang: Language, setLang: (l: Language) => void, flags: Record<string, string> }) => {
  const [isOpen, setIsOpen] = useState(false);
  const languages: Language[] = ['en', 'es', 'fr', 'de', 'it', 'pt'];

  return (
    <div className="relative">
      <button 
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-3 bg-[var(--surface-variant)] rounded-xl px-4 py-2.5 border border-[var(--border-color)] hover:border-neon-blue transition-all active:scale-95"
      >
        <span className={`fi ${flags[lang]} rounded-sm shadow-sm`}></span>
        <span className="text-[10px] font-black uppercase tracking-widest text-[var(--text-color)]">{lang}</span>
        <ChevronRight size={14} className={`text-[var(--on-surface-variant)] transition-transform ${isOpen ? 'rotate-90' : ''}`} />
      </button>

      {isOpen && (
        <>
          <div className="fixed inset-0 z-[60]" onClick={() => setIsOpen(false)} />
          <motion.div 
            initial={{ opacity: 0, y: 10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            className="absolute right-0 mt-2 w-40 bg-[var(--surface-dark)] border border-[var(--border-color)] rounded-2xl shadow-2xl z-[70] overflow-hidden py-2"
          >
            {languages.map((l) => (
              <button
                key={l}
                onClick={() => {
                  setLang(l);
                  setIsOpen(false);
                }}
                className={`w-full flex items-center gap-3 px-4 py-3 hover:bg-[var(--surface-variant)] transition-colors ${lang === l ? 'bg-neon-blue/10 text-neon-blue' : 'text-[var(--text-color)]'}`}
              >
                <span className={`fi ${flags[l]} rounded-sm shadow-sm`}></span>
                <span className="text-[10px] font-black uppercase tracking-widest">{l === 'en' ? 'English' : l === 'es' ? 'Español' : l === 'fr' ? 'Français' : l === 'de' ? 'Deutsch' : l === 'it' ? 'Italiano' : 'Português'}</span>
              </button>
            ))}
          </motion.div>
        </>
      )}
    </div>
  );
};

export const LandingPage = () => {
  const [lang, setLang] = useState<Language>('en');
  const [theme, setTheme] = useState<Theme>('dark');
  const t = translations[lang];

  useEffect(() => {
    const root = window.document.documentElement;
    if (theme === 'light') {
      root.classList.add('light');
    } else {
      root.classList.remove('light');
    }
  }, [theme]);

  const logoSrc = theme === 'dark' ? '/input_file_0.png' : '/input_file_1.png';
  const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' });
  const scrollToBottom = () => window.scrollTo({ top: document.documentElement.scrollHeight, behavior: 'smooth' });

  const flags = { en: "fi-us", es: "fi-es", fr: "fi-fr", de: "fi-de", it: "fi-it", pt: "fi-pt" };

  return (
    <div className="min-h-screen bg-[var(--bg-color)] text-[var(--text-color)] selection:bg-neon-blue/30 overflow-x-hidden transition-colors duration-500">
      {/* Scroll Controls */}
      <div className="fixed right-6 bottom-24 z-[60] flex flex-col gap-2">
        <button onClick={scrollToTop} className="p-3 rounded-xl bg-[var(--surface-dark)] border border-[var(--border-color)] text-[var(--on-surface-variant)] hover:text-neon-blue shadow-2xl transition-all active:scale-90"><ArrowUp size={20} /></button>
        <button onClick={scrollToBottom} className="p-3 rounded-xl bg-[var(--surface-dark)] border border-[var(--border-color)] text-[var(--on-surface-variant)] hover:text-neon-blue shadow-2xl transition-all active:scale-90"><ArrowDown size={20} /></button>
      </div>

      {/* Navigation */}
      <nav className="fixed top-0 w-full z-50 bg-[var(--bg-color)]/50 backdrop-blur-xl border-b border-[var(--border-color)] px-6 py-4">
        <div className="max-w-7xl mx-auto flex justify-between items-center">
          <button onClick={scrollToTop} className="flex items-center gap-3 group text-left">
            <div className="w-12 h-12 flex items-center justify-center overflow-hidden group-hover:scale-110 transition-transform">
              <img src={logoSrc} alt="Logo" className="w-full h-full object-contain" />
            </div>
            <div className="flex flex-col -space-y-1">
              <span className="font-headline font-bold text-lg tracking-tighter uppercase leading-none group-hover:text-neon-blue transition-colors">
                MultiTimer <span className="font-black">PRO</span>
              </span>
              <span className="text-[8px] font-black text-neon-blue tracking-[0.3em] uppercase opacity-70">Operator Terminal</span>
            </div>
          </button>
          
          <div className="flex items-center gap-4 sm:gap-6">
            <div className="hidden lg:flex items-center gap-8 text-[10px] font-bold uppercase tracking-widest text-[var(--on-surface-variant)]">
              <a href="#showcase" className="hover:text-neon-blue transition-colors">{t.showcase.title}</a>
              <a href="#features" className="hover:text-neon-blue transition-colors">{t.nav.features}</a>
              <a href="#testimonials" className="hover:text-neon-blue transition-colors">{t.testimonials.title}</a>
              <a href="#tech" className="hover:text-neon-blue transition-colors">{t.nav.tech}</a>
              <a href="#faq" className="hover:text-neon-blue transition-colors">{t.faq.title}</a>
            </div>
            <div className="flex items-center gap-2">
              <button onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')} className="p-2.5 rounded-xl bg-[var(--surface-variant)] border border-[var(--border-color)] text-[var(--on-surface-variant)] hover:text-neon-blue transition-all active:scale-90">
                {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
              </button>
              <LanguageSelector lang={lang} setLang={setLang} flags={flags} />
            </div>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section className="relative pt-48 pb-20 px-6 overflow-hidden">
        <div className="max-w-7xl mx-auto text-center relative z-10">
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.8 }}>
            <div className="flex justify-center mb-16">
              <div className="w-40 h-40 md:w-64 md:h-64 flex items-center justify-center p-6"><img src={logoSrc} alt="Large Logo" className="w-full h-full object-contain" /></div>
            </div>
            <span className="inline-block px-4 py-1.5 rounded-full bg-[var(--surface-variant)] border border-[var(--border-color)] text-[10px] font-black text-neon-blue uppercase tracking-[0.3em] mb-8">{t.hero.version}</span>
            <h1 className="font-headline text-6xl md:text-9xl font-black tracking-tighter leading-[0.85] mb-8 uppercase text-[var(--text-color)]">{t.hero.title1} <br /> <span className="text-transparent bg-clip-text bg-gradient-to-r from-neon-blue via-[var(--text-color)] to-neon-green">{t.hero.title2}</span></h1>
            <p className="max-w-2xl mx-auto text-[var(--on-surface-variant)] text-lg md:text-xl mb-12 leading-relaxed">{t.hero.subtitle}</p>
            <div className="flex justify-center">
              <button className="bg-[var(--text-color)] text-[var(--bg-color)] px-10 py-5 rounded-2xl font-black uppercase tracking-widest text-sm flex items-center justify-center gap-3 hover:bg-neon-blue hover:text-deep-black transition-all group shadow-2xl active:scale-95">
                <img src="/play_logo.png" alt="Google Play" className="w-6 h-6 object-contain" onError={(e) => e.currentTarget.style.display = 'none'} />
                {t.hero.cta}<ChevronRight size={18} className="group-hover:translate-x-1 transition-transform" />
              </button>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Visual Showcase */}
      <section id="showcase" className="py-32 px-6">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-24">
            <h2 className="font-headline text-4xl md:text-6xl font-black uppercase tracking-tighter mb-6 text-[var(--text-color)]">{t.showcase.title}</h2>
            <div className="h-1 w-24 bg-neon-blue mx-auto mb-6" />
            <p className="text-[var(--on-surface-variant)] uppercase tracking-[0.2em] text-xs font-bold">{t.showcase.subtitle}</p>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-10">
            <ShowcaseItem title={t.showcase.timers} img={theme === 'dark' ? "/input_file_3.png" : "/input_file_3_light.png"} color="#69DAFF" theme={theme} />
            <ShowcaseItem title={t.showcase.live} img={theme === 'dark' ? "/input_file_2.png" : "/input_file_2_light.png"} color="#2FF801" theme={theme} />
            <ShowcaseItem title={t.showcase.stats} img={theme === 'dark' ? "/input_file_4.png" : "/input_file_4_light.png"} color="#89A5FF" theme={theme} />
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section id="features" className="py-32 px-6 bg-[var(--surface-dark)]/30">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-20">
            <h2 className="font-headline text-4xl md:text-5xl font-black uppercase tracking-tighter mb-6 text-[var(--text-color)]">{t.features.title}</h2>
            <div className="h-1 w-24 bg-neon-blue mx-auto mb-6" />
            <p className="text-[var(--on-surface-variant)] uppercase tracking-widest text-xs font-bold">{t.features.subtitle}</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <FeatureCard icon={BookOpen} title={t.features.stitch.title} description={t.features.stitch.desc} color="#89A5FF" />
            <FeatureCard icon={Zap} title={t.features.precision.title} description={t.features.precision.desc} color="#2FF801" />
            <FeatureCard icon={Shield} title={t.features.security.title} description={t.features.security.desc} color="#69DAFF" />
          </div>
        </div>
      </section>

      {/* Testimonials - Infinite Scroll */}
      <section id="testimonials" className="py-32 overflow-hidden bg-[var(--surface-dark)]/20 border-y border-[var(--border-color)]">
        <div className="max-w-7xl mx-auto px-6 mb-16 text-center">
          <h2 className="font-headline text-4xl md:text-5xl font-black uppercase tracking-tighter text-[var(--text-color)] mb-6">{t.testimonials.title}</h2>
          <div className="h-1 w-24 bg-neon-blue mx-auto mb-6" />
          <p className="text-[var(--on-surface-variant)] uppercase tracking-widest text-xs font-bold">{t.testimonials.subtitle}</p>
        </div>
        <div className="relative flex">
          <motion.div animate={{ x: ["0%", "-50%"] }} transition={{ duration: 40, ease: "linear", repeat: Infinity }} className="flex gap-8 px-4">
            {[...t.testimonials.items, ...t.testimonials.items].map((item, i) => (
              <div key={i} className="w-[350px] flex-shrink-0 bg-[var(--surface-dark)] p-8 rounded-3xl border border-[var(--border-color)] relative">
                <Quote className="text-neon-blue/20 absolute top-6 right-8" size={32} />
                <p className="text-base font-medium italic mb-8 relative z-10 leading-relaxed text-[var(--text-color)]">"{item.quote}"</p>
                <div className="flex items-center gap-4 border-t border-[var(--border-color)] pt-6">
                  <div className="w-10 h-10 rounded-full bg-surface-variant flex items-center justify-center text-neon-blue"><User size={20} /></div>
                  <div>
                    <p className="font-headline font-bold text-sm uppercase tracking-widest text-[var(--text-color)]">{item.author}</p>
                    <p className="text-[8px] font-black text-neon-blue uppercase tracking-[0.2em] opacity-70">Verified Operator</p>
                  </div>
                </div>
              </div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* Tech Section */}
      <section id="tech" className="py-32 px-6 overflow-hidden">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center gap-20">
          <div className="flex-1 space-y-8">
            <span className="text-neon-green font-headline text-xs font-bold tracking-[0.4em] uppercase">{t.tech.label}</span>
            <h2 className="font-headline text-5xl md:text-7xl font-black uppercase leading-[0.9] tracking-tighter text-[var(--text-color)]">
              {t.tech.title1} <br /> <span className="text-[var(--on-surface-variant)]">{t.tech.title2}</span>
            </h2>
            <p className="text-[var(--on-surface-variant)] text-lg leading-relaxed">{t.tech.desc}</p>
            <div className="space-y-6">
              {[
                { icon: Timer, label: t.tech.concurrent, value: t.tech.unlimited },
                { icon: BarChart3, label: t.tech.retention, value: t.tech.infinite },
                { icon: Globe, label: t.tech.presets, value: t.tech.cloud }
              ].map((item, i) => (
                <div key={i} className="flex items-center gap-4 group">
                  <div className="w-12 h-12 rounded-2xl bg-[var(--surface-variant)] flex items-center justify-center text-[var(--on-surface-variant)] group-hover:text-neon-blue transition-all border border-[var(--border-color)]"><item.icon size={24} /></div>
                  <div>
                    <p className="text-[10px] font-bold text-[var(--on-surface-variant)] uppercase tracking-widest">{item.label}</p>
                    <p className="font-headline text-2xl font-bold text-[var(--text-color)]">{item.value}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
          <div className="flex-1 relative">
            <div className="relative z-10 bg-[var(--surface-dark)] border border-[var(--border-color)] p-10 rounded-[40px] shadow-2xl">
              <div className="flex justify-between items-center mb-12">
                <div className="space-y-1">
                  <p className="text-[10px] font-bold text-[var(--on-surface-variant)] uppercase tracking-widest">System Status</p>
                  <h4 className="font-headline text-2xl font-bold uppercase text-[var(--text-color)]">Operational</h4>
                </div>
                <div className="w-12 h-12 rounded-full bg-neon-green/10 flex items-center justify-center"><Zap className="text-neon-green" size={24} /></div>
              </div>
              <div className="space-y-8">
                {[75, 45, 90].map((val, i) => (
                  <div key={i} className="space-y-2">
                    <div className="flex justify-between text-[10px] font-bold uppercase tracking-widest text-[var(--on-surface-variant)]"><span>Module {i + 1}</span><span>{val}%</span></div>
                    <div className="h-2 bg-[var(--surface-variant)] rounded-full overflow-hidden">
                      <motion.div initial={{ width: 0 }} whileInView={{ width: `${val}%` }} className="h-full bg-neon-blue shadow-[0_0_10px_#69DAFF]" />
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[120%] h-[120%] border border-[var(--border-color)] rounded-full opacity-20" />
          </div>
        </div>
      </section>

      {/* FAQ */}
      <section id="faq" className="py-32 px-6">
        <div className="max-w-3xl mx-auto">
          <div className="text-center mb-20 space-y-4">
            <h2 className="font-headline text-4xl md:text-5xl font-black uppercase tracking-tighter text-[var(--text-color)] mb-6">{t.faq.title}</h2>
            <div className="h-1 w-24 bg-neon-blue mx-auto mb-6" />
            <p className="text-[var(--on-surface-variant)] uppercase tracking-widest text-xs font-bold">{t.faq.subtitle}</p>
          </div>
          <div className="bg-[var(--surface-dark)] p-8 md:p-12 rounded-[40px] border border-[var(--border-color)] shadow-2xl">
            {t.faq.items.map((item, i) => (
              <FAQItem key={i} question={item.q} answer={item.a} />
            ))}
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer id="download" className="py-24 px-6 border-t border-[var(--border-color)] bg-[var(--surface-dark)]/10">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row justify-between items-center gap-12 text-center md:text-left">
          <div className="space-y-6">
            <button onClick={scrollToTop} className="flex items-center justify-center md:justify-start gap-4 group">
              <div className="w-12 h-12 overflow-hidden"><img src={logoSrc} alt="Footer Logo" className="w-full h-full object-contain" /></div>
              <span className="font-headline font-bold text-xl tracking-tighter uppercase text-[var(--text-color)] group-hover:text-neon-blue transition-colors">MultiTimer <span className="font-black">PRO</span></span>
            </button>
            <p className="text-[var(--on-surface-variant)] text-[10px] uppercase tracking-[0.3em] font-bold">{t.footer.rights}</p>
          </div>
          <button className="bg-[var(--text-color)] text-[var(--bg-color)] px-12 py-6 rounded-2xl font-black uppercase tracking-widest text-sm flex items-center justify-center gap-3 hover:bg-neon-blue hover:text-deep-black transition-all group shadow-2xl active:scale-95">
            <img src="/play_logo.png" alt="Google Play" className="w-6 h-6 object-contain" onError={(e) => e.currentTarget.style.display = 'none'} />
            {t.hero.cta}<ChevronRight size={18} className="group-hover:translate-x-1 transition-transform" />
          </button>
        </div>
      </footer>
    </div>
  );
};
