import React, { useState } from 'react';
import { 
  Sparkles, 
  Video, 
  Ghost, 
  Upload, 
  Type, 
  Wand2, 
  Download, 
  Play, 
  Check, 
  Loader2, 
  Info,
  Film
} from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState('ideias');
  
  // States for AI Generators
  const [ideas, setIdeas] = useState([]);
  const [hooks, setHooks] = useState([]);
  const [loadingIdeas, setLoadingIdeas] = useState(false);
  const [loadingHooks, setLoadingHooks] = useState(false);

  // States for Mockup Editor
  const [videoUploaded, setVideoUploaded] = useState(false);
  const [videoName, setVideoName] = useState('');
  const [captionsEnabled, setCaptionsEnabled] = useState(false);
  const [glowEnabled, setGlowEnabled] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [exportComplete, setExportComplete] = useState(false);

  const fetchAIContent = async (type) => {
    const isIdeas = type === 'ideias';
    if (isIdeas) setLoadingIdeas(true);
    else setLoadingHooks(true);

    const apiKey = ""; // API key will be provided by Canvas runtime
    const apiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=${apiKey}`;

    const promptIdeas = "Gere 3 ideias de vídeos altamente criativos para vender semijoias no TikTok sem que a pessoa precise mostrar o rosto (faceless). Foque em ASMR, detalhes macro (close-ups), unboxing ou POVs estéticos. Responda em português do Brasil.";
    const promptHooks = "Gere 3 frases de gancho (hooks) agressivos, virais e muito chamativos para os primeiros 3 segundos de um vídeo no TikTok vendendo semijoias. O objetivo é prender a atenção de quem compra por impulso. Responda em português do Brasil.";

    const schemaIdeas = {
      type: "ARRAY",
      items: {
        type: "OBJECT",
        properties: {
          "titulo": { "type": "STRING", "description": "Título curto da ideia" },
          "detalhes": { "type": "STRING", "description": "Como gravar o vídeo na prática" }
        }
      }
    };

    const schemaHooks = {
      type: "ARRAY",
      items: {
        type: "OBJECT",
        properties: {
          "frase": { "type": "STRING", "description": "A frase exata para falar ou escrever no vídeo" },
          "porque_funciona": { "type": "STRING", "description": "Breve explicação psicológica do porquê esse gancho retém a atenção" }
        }
      }
    };

    const payload = {
      contents: [{ role: "user", parts: [{ text: isIdeas ? promptIdeas : promptHooks }] }],
      generationConfig: {
        responseMimeType: "application/json",
        responseSchema: isIdeas ? schemaIdeas : schemaHooks,
      }
    };

    try {
      const response = await fetch(apiUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = await response.json();
      
      if (data.candidates && data.candidates.length > 0) {
        const jsonText = data.candidates[0].content.parts[0].text;
        const parsed = JSON.parse(jsonText);
        if (isIdeas) setIdeas(parsed);
        else setHooks(parsed);
      }
    } catch (error) {
      console.error("Erro ao gerar conteúdo:", error);
    } finally {
      if (isIdeas) setLoadingIdeas(false);
      else setLoadingHooks(false);
    }
  };

  const handleFileUpload = (e) => {
    if (e.target.files && e.target.files[0]) {
      setVideoName(e.target.files[0].name);
      setVideoUploaded(true);
      setExportComplete(false);
    }
  };

  const handleExport = () => {
    if (!videoUploaded) return;
    setIsExporting(true);
    setExportComplete(false);
    // Simulate export delay
    setTimeout(() => {
      setIsExporting(false);
      setExportComplete(true);
    }, 2500);
  };

  const navItems = [
    { id: 'ideias', label: 'Ideias sem Aparecer', icon: Ghost },
    { id: 'hooks', label: 'Ganchos Virais', icon: Sparkles },
    { id: 'editor', label: 'Editor (Mockup)', icon: Film },
  ];

  return (
    <div className="flex h-screen bg-neutral-950 text-gray-100 font-sans overflow-hidden">
      
      {/* Sidebar */}
      <aside className="w-64 bg-neutral-900 border-r border-neutral-800 flex flex-col">
        <div className="p-6">
          <h1 className="text-xl font-bold bg-gradient-to-r from-purple-400 to-pink-500 bg-clip-text text-transparent flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-pink-500" />
            Creator Studio
          </h1>
          <p className="text-xs text-neutral-400 mt-1 uppercase tracking-wider font-semibold">Semijoias Edition</p>
        </div>

        <nav className="flex-1 px-4 space-y-2">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 ${
                  isActive 
                    ? 'bg-gradient-to-r from-purple-600/20 to-pink-600/20 text-pink-400 border border-pink-500/30' 
                    : 'text-neutral-400 hover:bg-neutral-800 hover:text-gray-200'
                }`}
              >
                <Icon className={`w-5 h-5 ${isActive ? 'text-pink-400' : ''}`} />
                <span className="font-medium text-sm">{item.label}</span>
              </button>
            );
          })}
        </nav>
        
        <div className="p-6 border-t border-neutral-800">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-purple-500 to-pink-500 flex items-center justify-center text-white font-bold">
              VS
            </div>
            <div>
              <p className="text-sm font-medium text-gray-200">Você</p>
              <p className="text-xs text-pink-400">Plano Pro</p>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto relative">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-purple-900/10 via-neutral-950 to-neutral-950 pointer-events-none"></div>
        
        <div className="p-10 max-w-5xl mx-auto relative z-10">
          
          {/* TAB 1: Ideias sem Aparecer */}
          {activeTab === 'ideias' && (
            <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
              <div>
                <h2 className="text-3xl font-bold text-white mb-2">Conteúdo Faceless</h2>
                <p className="text-neutral-400">Ideias de vídeos onde o foco é 100% na beleza da semijoia, sem precisar mostrar o rosto.</p>
              </div>
              
              <button 
                onClick={() => fetchAIContent('ideias')}
                disabled={loadingIdeas}
                className="flex items-center gap-2 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white px-6 py-3 rounded-lg font-semibold shadow-lg shadow-pink-900/20 transition-all disabled:opacity-70"
              >
                {loadingIdeas ? <Loader2 className="w-5 h-5 animate-spin" /> : <Wand2 className="w-5 h-5" />}
                {loadingIdeas ? 'Gerando ideias mágicas...' : 'Gerar Novas Ideias com IA'}
              </button>

              <div className="grid gap-4 mt-8">
                {ideas.map((idea, index) => (
                  <div key={index} className="bg-neutral-900 border border-neutral-800 rounded-xl p-6 hover:border-pink-500/50 transition-colors">
                    <div className="flex items-start gap-4">
                      <div className="bg-pink-500/10 p-3 rounded-lg text-pink-400">
                        <Video className="w-6 h-6" />
                      </div>
                      <div>
                        <h3 className="text-lg font-semibold text-gray-100 mb-2">{idea.titulo}</h3>
                        <p className="text-neutral-400 leading-relaxed">{idea.detalhes}</p>
                      </div>
                    </div>
                  </div>
                ))}
                {!loadingIdeas && ideas.length === 0 && (
                  <div className="text-center py-20 border-2 border-dashed border-neutral-800 rounded-xl text-neutral-500">
                    <Ghost className="w-12 h-12 mx-auto mb-3 opacity-20" />
                    <p>Clique no botão acima para gerar suas primeiras ideias de vídeos.</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* TAB 2: Ganchos Virais */}
          {activeTab === 'hooks' && (
            <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
              <div>
                <h2 className="text-3xl font-bold text-white mb-2">Ganchos (Hooks) Agressivos</h2>
                <p className="text-neutral-400">Os primeiros 3 segundos são cruciais. Use essas frases no início do vídeo ou como texto na tela para prender a atenção.</p>
              </div>
              
              <button 
                onClick={() => fetchAIContent('hooks')}
                disabled={loadingHooks}
                className="flex items-center gap-2 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white px-6 py-3 rounded-lg font-semibold shadow-lg shadow-purple-900/20 transition-all disabled:opacity-70"
              >
                {loadingHooks ? <Loader2 className="w-5 h-5 animate-spin" /> : <Sparkles className="w-5 h-5" />}
                {loadingHooks ? 'Criando roteiros...' : 'Gerar Ganchos Virais com IA'}
              </button>

              <div className="grid gap-4 mt-8">
                {hooks.map((hook, index) => (
                  <div key={index} className="bg-neutral-900 border border-neutral-800 rounded-xl p-6 relative overflow-hidden group">
                    <div className="absolute left-0 top-0 bottom-0 w-1 bg-gradient-to-b from-purple-500 to-pink-500"></div>
                    <h3 className="text-xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-gray-100 to-gray-300 mb-3 ml-2">
                      "{hook.frase}"
                    </h3>
                    <div className="bg-neutral-950 p-4 rounded-lg ml-2 border border-neutral-800/50 flex items-start gap-3">
                      <Info className="w-5 h-5 text-purple-400 shrink-0 mt-0.5" />
                      <p className="text-sm text-neutral-400">{hook.porque_funciona}</p>
                    </div>
                  </div>
                ))}
                {!loadingHooks && hooks.length === 0 && (
                  <div className="text-center py-20 border-2 border-dashed border-neutral-800 rounded-xl text-neutral-500">
                    <Type className="w-12 h-12 mx-auto mb-3 opacity-20" />
                    <p>Gere ganchos magnéticos e veja suas visualizações aumentarem.</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* TAB 3: Editor Mockup */}
          {activeTab === 'editor' && (
            <div className="h-full flex flex-col animate-in fade-in slide-in-from-bottom-4 duration-500">
              
              <div className="mb-6 flex justify-between items-end">
                <div>
                  <h2 className="text-3xl font-bold text-white mb-2">TikTok Export Studio</h2>
                  <p className="text-neutral-400">Adicione legendas e o filtro "Glow" nas suas joias em um clique.</p>
                </div>
                <div className="bg-neutral-900/50 border border-amber-500/20 px-4 py-2 rounded-lg flex items-center gap-2 max-w-sm">
                  <Info className="w-4 h-4 text-amber-500 shrink-0" />
                  <p className="text-xs text-amber-500/80 leading-tight">Nota: Esta é uma interface de demonstração (Mockup). O processamento e renderização real de vídeo exige um backend (servidor) dedicado.</p>
                </div>
              </div>

              <div className="flex gap-8 mt-4">
                
                {/* Video Player Area */}
                <div className="w-[340px] h-[600px] bg-black rounded-2xl border border-neutral-800 relative overflow-hidden flex-shrink-0 shadow-2xl shadow-pink-900/10">
                  {!videoUploaded ? (
                    <div className="absolute inset-0 flex flex-col items-center justify-center text-neutral-500 p-6 text-center">
                      <Upload className="w-12 h-12 mb-4 opacity-50 text-pink-500" />
                      <p className="font-medium text-gray-300 mb-1">Nenhum vídeo carregado</p>
                      <p className="text-sm">Faça o upload do vídeo da semijoia na proporção 9:16.</p>
                    </div>
                  ) : (
                    <div className="absolute inset-0 bg-neutral-900 flex items-center justify-center group">
                      {/* Simulating Video Image/Preview */}
                      <div className="absolute inset-0 bg-gradient-to-br from-neutral-800 to-neutral-950 opacity-50"></div>
                      <Play className="w-16 h-16 text-white/50 group-hover:text-white/80 transition-all cursor-pointer z-10" />
                      
                      {/* Simulated Effects overlays */}
                      {glowEnabled && (
                        <div className="absolute inset-0 bg-pink-500/10 mix-blend-screen pointer-events-none"></div>
                      )}
                      {captionsEnabled && (
                        <div className="absolute bottom-24 left-0 right-0 text-center z-20">
                          <span className="bg-black/70 text-white font-bold px-4 py-2 rounded-lg text-lg inline-block backdrop-blur-sm shadow-[0_0_10px_rgba(236,72,153,0.5)]">
                            ✨ OLHA ESSE BRILHO ✨
                          </span>
                        </div>
                      )}
                    </div>
                  )}
                </div>

                {/* Editor Controls */}
                <div className="flex-1 space-y-6">
                  
                  {/* Upload Card */}
                  <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-6">
                    <label className="cursor-pointer flex flex-col items-center justify-center w-full h-32 border-2 border-dashed border-neutral-700 hover:border-pink-500/50 rounded-lg bg-neutral-950/50 transition-colors">
                      <div className="flex flex-col items-center justify-center pt-5 pb-6">
                        <Upload className="w-8 h-8 text-neutral-400 mb-2" />
                        <p className="text-sm text-neutral-400 font-medium">
                          {videoUploaded ? "Trocar Vídeo" : "Clique para carregar o arquivo"}
                        </p>
                      </div>
                      <input type="file" className="hidden" accept="video/mp4,video/x-m4v,video/*" onChange={handleFileUpload} />
                    </label>
                    {videoUploaded && (
                      <p className="text-sm text-pink-400 mt-3 flex items-center gap-2">
                        <Check className="w-4 h-4" /> Arquivo: {videoName}
                      </p>
                    )}
                  </div>

                  {/* Effects Controls */}
                  <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-6 space-y-6">
                    <h3 className="font-semibold text-gray-200">Efeitos e Melhorias</h3>
                    
                    {/* Toggle: Captions */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="bg-neutral-800 p-2 rounded-lg"><Type className="w-5 h-5 text-gray-300" /></div>
                        <div>
                          <p className="font-medium text-gray-200">Legendas Automáticas</p>
                          <p className="text-xs text-neutral-400">Transcreve o áudio estilo TikTok</p>
                        </div>
                      </div>
                      <button 
                        onClick={() => setCaptionsEnabled(!captionsEnabled)}
                        className={`w-12 h-6 rounded-full transition-colors relative ${captionsEnabled ? 'bg-pink-500' : 'bg-neutral-700'}`}
                      >
                        <div className={`w-4 h-4 rounded-full bg-white absolute top-1 transition-transform ${captionsEnabled ? 'left-7' : 'left-1'}`}></div>
                      </button>
                    </div>

                    <div className="h-px bg-neutral-800 w-full"></div>

                    {/* Toggle: Glow */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className="bg-neutral-800 p-2 rounded-lg"><Sparkles className="w-5 h-5 text-amber-300" /></div>
                        <div>
                          <p className="font-medium text-gray-200">Filtro Brilho (Glow)</p>
                          <p className="text-xs text-neutral-400">Realça pedras e banho de ouro/prata</p>
                        </div>
                      </div>
                      <button 
                        onClick={() => setGlowEnabled(!glowEnabled)}
                        className={`w-12 h-6 rounded-full transition-colors relative ${glowEnabled ? 'bg-pink-500' : 'bg-neutral-700'}`}
                      >
                        <div className={`w-4 h-4 rounded-full bg-white absolute top-1 transition-transform ${glowEnabled ? 'left-7' : 'left-1'}`}></div>
                      </button>
                    </div>
                  </div>

                  {/* Export Button */}
                  <div className="pt-4">
                    <button 
                      onClick={handleExport}
                      disabled={!videoUploaded || isExporting}
                      className="w-full bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white font-bold py-4 px-6 rounded-xl flex items-center justify-center gap-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-pink-900/20"
                    >
                      {isExporting ? (
                        <>
                          <Loader2 className="w-6 h-6 animate-spin" />
                          Processando Vídeo (Simulação)...
                        </>
                      ) : exportComplete ? (
                        <>
                          <Check className="w-6 h-6" />
                          Pronto para o TikTok!
                        </>
                      ) : (
                        <>
                          <Download className="w-6 h-6" />
                          Exportar para TikTok
                        </>
                      )}
                    </button>
                  </div>

                </div>
              </div>
            </div>
          )}

        </div>
      </main>
    </div>
  );
}