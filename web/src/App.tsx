/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from 'react';
import { 
  Timer, 
  History, 
  BarChart3, 
  Settings, 
  Plus, 
  Play, 
  Pause, 
  RotateCcw,
  BookOpen,
  Dumbbell,
  Coffee,
  Moon,
  ChevronRight,
  TrendingUp,
  Award,
  Zap,
  Heart,
  Globe
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { LandingPage } from './components/LandingPage';

// --- Types ---
type Screen = 'timers' | 'presets' | 'stats' | 'settings' | 'live';

interface TimerItem {
  id: string;
  name: string;
  category: string;
  time: string;
  millis?: string;
  progress: number;
  status: 'LIVE' | 'READY' | 'PAUSED' | 'IDLE';
  color: string;
}

// --- Components ---

const BottomNav = ({ active, onChange }: { active: Screen, onChange: (s: Screen) => void }) => {
  const items = [
    { id: 'timers', icon: Timer, label: 'Timers' },
    { id: 'presets', icon: BookOpen, label: 'Presets' },
    { id: 'stats', icon: BarChart3, label: 'Stats' },
    { id: 'settings', icon: Settings, label: 'Settings' },
  ];

  return (
    <nav className="fixed bottom-0 w-full bg-deep-black/90 backdrop-blur-2xl border-t border-white/5 h-20 px-6 flex justify-around items-center z-50">
      {items.map((item) => (
        <button
          key={item.id}
          onClick={() => onChange(item.id as Screen)}
          className={`flex flex-col items-center gap-1 transition-all duration-300 relative ${
            active === item.id ? 'text-neon-blue' : 'text-on-surface-variant opacity-50'
          }`}
        >
          <item.icon size={24} strokeWidth={active === item.id ? 2.5 : 2} />
          <span className="text-[10px] font-medium uppercase tracking-widest">{item.label}</span>
          {active === item.id && (
            <motion.div 
              layoutId="nav-indicator"
              className="absolute -bottom-1 w-1 h-1 bg-neon-blue rounded-full shadow-[0_0_8px_#69DAFF]"
            />
          )}
        </button>
      ))}
    </nav>
  );
};

const Header = () => (
  <header className="fixed top-0 w-full bg-deep-black/80 backdrop-blur-xl h-16 px-6 flex justify-between items-center z-50 border-b border-white/5">
    <div className="flex items-center gap-2">
      <div className="w-8 h-8 bg-neon-blue/10 rounded-lg flex items-center justify-center">
        <Timer className="text-neon-blue" size={20} />
      </div>
      <h1 className="font-headline font-bold text-lg tracking-tighter text-neon-blue uppercase">
        MultiTimer <span className="font-black">PRO</span>
      </h1>
    </div>
    <div className="w-8 h-8 rounded-full border border-white/10 overflow-hidden">
      <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" alt="User" />
    </div>
  </header>
);

// --- Screens ---

const TimersScreen = ({ onTimerClick }: { onTimerClick: () => void }) => {
  const timers: TimerItem[] = [
    { id: '1', name: 'Workout', category: 'LIVE', time: '12:45', millis: '.28', progress: 0.66, status: 'LIVE', color: '#69DAFF' },
    { id: '2', name: 'Pomodoro', category: 'READY', time: '25:00', progress: 1, status: 'READY', color: '#2FF801' },
    { id: '3', name: 'Meditation', category: 'PAUSED', time: '08:12', progress: 0.33, status: 'PAUSED', color: '#69DAFF' },
  ];

  return (
    <div className="pt-24 pb-32 px-6 space-y-6">
      <div className="flex justify-between items-end">
        <div>
          <p className="text-[10px] font-bold text-on-surface-variant tracking-[0.2em] uppercase">Status</p>
          <h2 className="font-headline text-2xl font-bold">Active Instruments</h2>
        </div>
        <div className="bg-surface-variant px-3 py-1 rounded text-[10px] font-bold text-on-surface-variant">
          3 Active
        </div>
      </div>

      <div className="space-y-4">
        {timers.map((timer) => (
          <motion.div
            key={timer.id}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            onClick={onTimerClick}
            className="relative bg-surface-dark rounded-2xl p-5 border border-white/5 overflow-hidden cursor-pointer group"
          >
            <div 
              className="absolute left-0 top-0 h-full w-1.5" 
              style={{ backgroundColor: timer.color, boxShadow: `0 0 15px ${timer.color}` }}
            />
            <div className="flex justify-between items-center">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">{timer.name}</span>
                  <span className="text-[9px] font-black" style={{ color: timer.color }}>{timer.status}</span>
                </div>
                <div className="flex items-baseline gap-1">
                  <span className={`font-headline text-3xl font-bold ${timer.status === 'IDLE' ? 'text-on-surface-variant/50' : 'text-white'}`}>
                    {timer.time}
                  </span>
                  {timer.millis && <span className="text-sm text-on-surface-variant opacity-50">{timer.millis}</span>}
                </div>
                <div className="w-32 h-1 bg-surface-variant rounded-full mt-2 overflow-hidden">
                  <div 
                    className="h-full transition-all duration-500" 
                    style={{ width: `${timer.progress * 100}%`, backgroundColor: timer.color }}
                  />
                </div>
              </div>
              <div className="flex items-center gap-3">
                <button className="p-2 text-on-surface-variant hover:text-white transition-colors">
                  <RotateCcw size={20} />
                </button>
                <button 
                  className={`w-12 h-12 rounded-full flex items-center justify-center transition-all ${
                    timer.status === 'LIVE' ? 'bg-neon-blue text-deep-black shadow-[0_0_15px_#69DAFF]' : 'bg-surface-variant text-neon-blue'
                  }`}
                >
                  {timer.status === 'LIVE' ? <Pause size={24} fill="currentColor" /> : <Play size={24} fill="currentColor" />}
                </button>
              </div>
            </div>
          </motion.div>
        ))}
      </div>
      
      <button className="fixed bottom-24 right-6 w-14 h-14 bg-neon-blue rounded-2xl flex items-center justify-center text-deep-black shadow-[0_0_20px_rgba(105,218,255,0.4)] active:scale-90 transition-transform">
        <Plus size={32} />
      </button>
    </div>
  );
};

const PresetsScreen = () => {
  return (
    <div className="pt-24 pb-32 px-6 space-y-8">
      <h2 className="font-headline text-4xl font-bold tracking-tighter">Saved Presets</h2>
      
      <div className="grid grid-cols-2 gap-4">
        <div className="col-span-2 bg-surface-dark rounded-3xl p-6 border border-white/5 relative overflow-hidden group">
          <div className="absolute left-0 top-0 h-full w-2 bg-neon-blue shadow-[0_0_15px_#69DAFF]" />
          <div className="flex justify-between items-start mb-10">
            <div>
              <BookOpen className="text-neon-blue mb-4" size={32} />
              <h3 className="font-headline text-2xl font-bold">Focus 25m</h3>
              <p className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">Deep Work Session</p>
            </div>
            <span className="font-headline text-4xl font-bold text-neon-blue tracking-tighter">25:00</span>
          </div>
          <button className="w-full h-14 bg-neon-blue rounded-xl flex items-center justify-center gap-2 text-deep-black font-black uppercase tracking-widest shadow-[0_0_15px_rgba(105,218,255,0.3)]">
            <Play size={20} fill="currentColor" />
            Start Session
          </button>
        </div>

        <div className="bg-surface-high rounded-2xl p-5 border border-white/5 flex flex-col justify-between h-40">
          <div className="flex justify-between items-start">
            <Dumbbell className="text-neon-green" size={24} />
            <span className="text-[9px] font-black text-on-surface-variant uppercase tracking-widest">HIIT</span>
          </div>
          <div>
            <h4 className="font-headline font-bold text-lg">Quick Workout</h4>
            <div className="flex justify-between items-center mt-2">
              <span className="font-headline text-xl font-bold text-neon-green">10:00</span>
              <button className="w-8 h-8 rounded-full bg-surface-variant flex items-center justify-center text-neon-green">
                <Play size={16} fill="currentColor" />
              </button>
            </div>
          </div>
        </div>

        <div className="bg-surface-high rounded-2xl p-5 border border-white/5 flex flex-col justify-between h-40">
          <div className="flex justify-between items-start">
            <Coffee className="text-neon-purple" size={24} />
          </div>
          <div>
            <h4 className="font-headline font-bold text-lg">Tea Brew</h4>
            <div className="flex justify-between items-center mt-2">
              <span className="font-headline text-xl font-bold text-neon-purple">03:00</span>
              <button className="w-8 h-8 rounded-full bg-surface-variant flex items-center justify-center text-neon-purple">
                <Play size={16} fill="currentColor" />
              </button>
            </div>
          </div>
        </div>
        
        <div className="col-span-2 border-2 border-dashed border-white/10 rounded-2xl p-8 flex flex-col items-center justify-center gap-3 opacity-50">
          <Plus size={32} className="text-on-surface-variant" />
          <div className="text-center">
            <p className="font-bold text-sm">Add New Preset</p>
            <p className="text-[10px] uppercase tracking-widest">Design custom sequence</p>
          </div>
        </div>
      </div>
    </div>
  );
};

const StatsScreen = () => {
  const stats = [
    { label: 'Projects', value: 32, active: false },
    { label: 'Focus', value: 48, active: true },
    { label: 'Rest', value: 20, active: false },
    { label: 'Fitness', value: 56, active: false },
    { label: 'Home', value: 40, active: false },
  ];

  return (
    <div className="pt-24 pb-32 px-6 space-y-10">
      <div className="space-y-2">
        <p className="text-neon-blue font-headline text-xs font-bold tracking-[0.3em] uppercase">Precision Dashboard</p>
        <h2 className="font-headline text-5xl font-bold tracking-tighter leading-none">
          Weekly <br /> <span className="text-on-surface-variant">Insights.</span>
        </h2>
      </div>

      <div className="bg-surface-dark rounded-3xl p-8 border border-white/5 space-y-10">
        <div className="flex justify-between items-center">
          <h3 className="font-headline text-xl font-bold">Usage by Category</h3>
          <span className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">Mon — Sun</span>
        </div>
        
        <div className="h-48 flex items-end justify-between gap-2">
          {stats.map((stat) => (
            <div key={stat.label} className="flex-1 flex flex-col items-center gap-4 group">
              <div className="relative w-full flex flex-col items-center">
                <motion.div 
                  initial={{ height: 0 }}
                  animate={{ height: `${stat.value * 2}px` }}
                  className={`w-full rounded-t-lg transition-all duration-500 ${
                    stat.active ? 'bg-gradient-to-t from-neon-blue/20 to-neon-blue shadow-[0_0_15px_rgba(105,218,255,0.4)]' : 'bg-surface-variant'
                  }`}
                />
              </div>
              <span className={`text-[8px] font-black uppercase tracking-tighter ${stat.active ? 'text-neon-blue' : 'text-on-surface-variant'}`}>
                {stat.label}
              </span>
            </div>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4">
        <div className="bg-surface-high p-6 rounded-2xl border-l-4 border-neon-green flex items-start gap-4">
          <TrendingUp className="text-neon-green shrink-0" size={24} />
          <div>
            <h4 className="font-headline font-bold text-neon-green uppercase tracking-widest text-xs mb-2">Efficiency Insight</h4>
            <p className="text-sm leading-relaxed">
              You've increased your <span className="font-bold">Deep Work</span> time by <span className="text-neon-green font-bold">12%</span> compared to last week.
            </p>
          </div>
        </div>
        
        <div className="bg-surface-high p-6 rounded-2xl border-l-4 border-neon-blue flex items-start gap-4">
          <Zap className="text-neon-blue shrink-0" size={24} />
          <div>
            <h4 className="font-headline font-bold text-neon-blue uppercase tracking-widest text-xs mb-2">Pro Suggestion</h4>
            <p className="text-sm leading-relaxed">
              Your productivity peaks at <span className="text-neon-blue font-bold">10:30 AM</span>. Schedule your "Projects" timers then.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

const SettingsScreen = ({ onShowLanding }: { onShowLanding: () => void }) => {
  return (
    <div className="pt-24 pb-32 px-6 space-y-8">
      <div className="space-y-1">
        <h2 className="font-headline text-4xl font-bold tracking-tighter">Settings</h2>
        <p className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">Instrument Configuration</p>
      </div>

      <div className="bg-gradient-to-br from-neon-blue to-neon-blue-dim p-8 rounded-3xl flex items-center justify-between shadow-[0_0_20px_rgba(105,218,255,0.3)]">
        <div className="space-y-1">
          <h3 className="font-headline text-2xl font-bold text-deep-black">Support Dev</h3>
          <p className="text-deep-black/70 text-sm font-medium">Keep the pulse of time ticking</p>
        </div>
        <Heart className="text-deep-black" size={40} fill="currentColor" />
      </div>

      <div className="space-y-4">
        <button 
          onClick={onShowLanding}
          className="w-full bg-surface-high p-6 rounded-2xl border border-white/5 flex items-center justify-between hover:bg-white/5 transition-colors"
        >
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 bg-surface-variant rounded-xl flex items-center justify-center text-neon-blue">
              <Globe size={20} />
            </div>
            <div className="text-left">
              <p className="font-bold">View Landing Page</p>
              <p className="text-[10px] text-on-surface-variant uppercase">Publicity & Info</p>
            </div>
          </div>
          <ChevronRight size={20} className="text-on-surface-variant" />
        </button>

        <div className="bg-surface-dark p-6 rounded-2xl border border-white/5 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 bg-surface-variant rounded-xl flex items-center justify-center text-neon-blue">
              <Zap size={20} />
            </div>
            <div>
              <p className="font-bold">Cloud Sync</p>
              <p className="text-[10px] text-on-surface-variant uppercase">Across all devices</p>
            </div>
          </div>
          <div className="w-12 h-6 bg-neon-green/20 rounded-full relative p-1">
            <div className="w-4 h-4 bg-neon-green rounded-full absolute right-1" />
          </div>
        </div>

        <div className="bg-surface-dark p-6 rounded-2xl border border-white/5 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 bg-surface-variant rounded-xl flex items-center justify-center text-neon-blue">
              <Award size={20} />
            </div>
            <div>
              <p className="font-bold">Critical Alerts</p>
              <p className="text-[10px] text-on-surface-variant uppercase">Bypass silent mode</p>
            </div>
          </div>
          <div className="w-12 h-6 bg-surface-variant rounded-full relative p-1">
            <div className="w-4 h-4 bg-on-surface-variant rounded-full absolute left-1" />
          </div>
        </div>
      </div>

      <div className="bg-surface-dark p-8 rounded-3xl border border-white/5 space-y-6">
        <div className="flex items-center gap-3 mb-4">
          <TrendingUp className="text-neon-blue" size={20} />
          <h3 className="font-headline font-bold text-lg uppercase tracking-widest">Information</h3>
        </div>
        <div className="space-y-6">
          <div className="flex justify-between items-baseline">
            <span className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">Version</span>
            <span className="font-headline text-2xl font-bold">4.2.0 <span className="text-neon-blue text-xs ml-2">GOLD</span></span>
          </div>
          <div className="space-y-2">
            <span className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">Engine</span>
            <p className="text-sm leading-relaxed text-on-surface-variant">
              MultiTimer PRO uses a high-precision synchronization engine designed for critical performance environments.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

const LiveTimerScreen = ({ onBack }: { onBack: () => void }) => {
  return (
    <motion.div 
      initial={{ x: '100%' }}
      animate={{ x: 0 }}
      exit={{ x: '100%' }}
      className="fixed inset-0 bg-deep-black z-[60] pt-24 px-6 flex flex-col items-center"
    >
      <button 
        onClick={onBack}
        className="absolute top-8 left-6 text-on-surface-variant hover:text-white flex items-center gap-2"
      >
        <RotateCcw size={20} />
        <span className="text-[10px] font-bold uppercase tracking-widest">Back</span>
      </button>

      <div className="relative w-72 h-72 flex items-center justify-center mb-12">
        <svg className="absolute inset-0 w-full h-full -rotate-90">
          <circle 
            cx="50%" cy="50%" r="48%" 
            className="stroke-surface-variant fill-none" 
            strokeWidth="4" 
          />
          <motion.circle 
            cx="50%" cy="50%" r="48%" 
            className="stroke-neon-green fill-none" 
            strokeWidth="4" 
            strokeDasharray="600"
            initial={{ strokeDashoffset: 600 }}
            animate={{ strokeDashoffset: 150 }}
            transition={{ duration: 2, ease: "easeInOut" }}
            strokeLinecap="round"
            style={{ filter: 'drop-shadow(0 0 8px #2FF801)' }}
          />
        </svg>
        <div className="text-center z-10">
          <p className="text-on-surface-variant text-[10px] font-bold tracking-[0.3em] uppercase mb-2">Focus Session</p>
          <h3 className="font-headline text-7xl font-bold tracking-tighter">24:58</h3>
          <p className="text-neon-green text-[10px] font-bold tracking-widest uppercase mt-2">Active</p>
        </div>
      </div>

      <div className="w-full space-y-8">
        <div className="flex justify-between items-end">
          <div className="space-y-1">
            <h4 className="font-headline text-3xl font-bold tracking-tight">Coffee <br /> Preparation</h4>
            <p className="text-on-surface-variant text-sm">V60 Method • Light Roast</p>
          </div>
          <div className="text-right">
            <p className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest mb-1">Goal</p>
            <p className="font-headline text-xl font-bold">30:00</p>
          </div>
        </div>

        <div className="flex gap-4">
          <button className="flex-1 h-16 bg-gradient-to-br from-neon-blue to-neon-blue-dim rounded-2xl text-deep-black font-black text-lg flex items-center justify-center gap-2 shadow-[0_0_15px_rgba(105,218,255,0.2)]">
            <Pause size={24} fill="currentColor" />
            Pause
          </button>
          <button className="w-16 h-16 bg-surface-variant rounded-2xl flex items-center justify-center text-white">
            <RotateCcw size={24} />
          </button>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="bg-surface-dark p-6 rounded-2xl border-b-2 border-neon-green/30">
            <TrendingUp className="text-neon-green mb-3" size={20} />
            <p className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest mb-1">Progress</p>
            <p className="font-headline text-2xl font-bold">83%</p>
          </div>
          <div className="bg-surface-dark p-6 rounded-2xl border-b-2 border-neon-blue/30">
            <Timer className="text-neon-blue mb-3" size={20} />
            <p className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest mb-1">Ends At</p>
            <p className="font-headline text-2xl font-bold">14:45</p>
          </div>
        </div>
      </div>
    </motion.div>
  );
};

// --- Main App ---

export default function App() {
  const [screen, setScreen] = useState<Screen>('timers');
  const [showLive, setShowLive] = useState(false);
  const [showLanding, setShowLanding] = useState(true);

  if (showLanding) {
    return <LandingPage />;
  }

  return (
    <div className="min-h-screen bg-deep-black text-white selection:bg-neon-blue/30">
      <Header />
      
      <main className="relative">
        <AnimatePresence mode="wait">
          {screen === 'timers' && (
            <motion.div
              key="timers"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
            >
              <TimersScreen onTimerClick={() => setShowLive(true)} />
            </motion.div>
          )}
          {screen === 'presets' && (
            <motion.div
              key="presets"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
            >
              <PresetsScreen />
            </motion.div>
          )}
          {screen === 'stats' && (
            <motion.div
              key="stats"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
            >
              <StatsScreen />
            </motion.div>
          )}
          {screen === 'settings' && (
            <motion.div
              key="settings"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
            >
              <SettingsScreen onShowLanding={() => setShowLanding(true)} />
            </motion.div>
          )}
        </AnimatePresence>

        <AnimatePresence>
          {showLive && <LiveTimerScreen onBack={() => setShowLive(false)} />}
        </AnimatePresence>
      </main>

      <BottomNav active={screen} onChange={setScreen} />
    </div>
  );
}
