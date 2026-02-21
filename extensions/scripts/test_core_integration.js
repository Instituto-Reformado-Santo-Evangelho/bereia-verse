
const fs = require('fs');
const path = require('path');
const vm = require('vm');

// --- 1. MOCK DOM ENVIRONMENT ---
const window = {
    ACF_CORE: {},
    console: console
};

const Node = {
    ELEMENT_NODE: 1,
    TEXT_NODE: 3
};

const NodeFilter = {
    SHOW_TEXT: 4,
    FILTER_ACCEPT: 1,
    FILTER_REJECT: 2,
    FILTER_SKIP: 3
};

class DOMTokenList {
    constructor() { this._classes = new Set(); }
    add(c) { this._classes.add(c); }
    contains(c) { return this._classes.has(c); }
    remove(c) { this._classes.delete(c); }
}

class Element {
    constructor(tagName) {
        this.nodeType = Node.ELEMENT_NODE;
        this.tagName = tagName.toUpperCase();
        this.classList = new DOMTokenList();
        this.childNodes = [];
        this.parentNode = null;
        this.style = {};
        this.dataset = {};
        this.attributes = {};
    }

    appendChild(node) {
        node.parentNode = this;
        this.childNodes.push(node);
        return node;
    }

    insertBefore(newNode, referenceNode) {
        newNode.parentNode = this;
        const index = this.childNodes.indexOf(referenceNode);
        if (index === -1) {
            this.childNodes.push(newNode);
        } else {
            this.childNodes.splice(index, 0, newNode);
        }
        return newNode;
    }

    removeChild(node) {
        const index = this.childNodes.indexOf(node);
        if (index > -1) {
            this.childNodes.splice(index, 1);
            node.parentNode = null;
        }
        return node;
    }

    replaceChild(newNode, oldNode) {
        const index = this.childNodes.indexOf(oldNode);
        if (index > -1) {
            newNode.parentNode = this;
            this.childNodes[index] = newNode;
            oldNode.parentNode = null;
        } else {
            throw new Error("Node not found");
        }
    }
    
    addEventListener(type, listener) {
        // No-op for mock
    }
    
    querySelector(selector) {
        // Very basic mock for test purposes (only supports finding self if body)
        if (selector === 'body' && this.tagName === 'BODY') return this;
        return null;
    }
    
    get previousSibling() {
        if (!this.parentNode) return null;
        const index = this.parentNode.childNodes.indexOf(this);
        if (index > 0) return this.parentNode.childNodes[index - 1];
        return null;
    }
    
    get textContent() {
        return this.childNodes.map(c => c.textContent).join('');
    }
    
    set textContent(v) {
        this.childNodes = [new Text(v)];
        this.childNodes[0].parentNode = this;
    }

    set className(v) {
        this.classList = new DOMTokenList();
        if (v) {
            v.split(' ').forEach(c => this.classList.add(c));
        }
    }
}

class Text {
    constructor(data) {
        this.nodeType = Node.TEXT_NODE;
        this.nodeValue = data;
        this.parentNode = null;
    }
    
    get textContent() { return this.nodeValue; }
    
    get previousSibling() {
        if (!this.parentNode) return null;
        const index = this.parentNode.childNodes.indexOf(this);
        if (index > 0) return this.parentNode.childNodes[index - 1];
        return null;
    }
}

class DocumentFragment {
    constructor() {
        this.nodeType = 11; // DOCUMENT_FRAGMENT_NODE
        this.childNodes = [];
    }
    appendChild(node) {
        node.parentNode = this; // Technically fragment is parent until moved
        this.childNodes.push(node);
        return node;
    }
}

const document = {
    createElement: (tag) => new Element(tag),
    createTextNode: (text) => new Text(text),
    createDocumentFragment: () => new DocumentFragment(),
    createTreeWalker: (root, whatToShow, filter) => {
        // Flatten the tree into a list of nodes to traverse
        const nodes = [];
        function traverse(node) {
            if (node.nodeType === Node.TEXT_NODE) {
                if (filter.acceptNode(node) === NodeFilter.FILTER_ACCEPT) {
                    nodes.push(node);
                }
            } else if (node.nodeType === Node.ELEMENT_NODE) {
                for (const child of node.childNodes) {
                    traverse(child);
                }
            }
        }
        traverse(root);
        
        let currentIdx = -1;
        const walker = {
            currentNode: null,
            nextNode: function() {
                currentIdx++;
                if (currentIdx < nodes.length) {
                    this.currentNode = nodes[currentIdx];
                    return this.currentNode;
                }
                return null;
            }
        };
        return walker;
    },
    body: new Element('BODY'),
    querySelector: (sel) => {
        if (sel === 'body') return document.body;
        return null;
    }
};

window.document = document;

// Handle replaceChild for Fragment (Move children of fragment to parent)
// Overwrite Element.replaceChild to handle fragment
const originalReplaceChild = Element.prototype.replaceChild;
Element.prototype.replaceChild = function(newChild, oldChild) {
    if (newChild instanceof DocumentFragment) {
        const index = this.childNodes.indexOf(oldChild);
        if (index === -1) throw new Error("Node not found");
        
        // Remove old
        oldChild.parentNode = null;
        
        // Insert fragment children
        const newNodes = newChild.childNodes.splice(0); // Empty the fragment
        newNodes.forEach(n => n.parentNode = this);
        
        this.childNodes.splice(index, 1, ...newNodes);
    } else {
        originalReplaceChild.call(this, newChild, oldChild);
    }
};


// --- 2. LOAD CORE SCRIPTS ---
const rootDir = path.resolve(__dirname, '..');
const coreDir = path.join(rootDir, 'core');

const bibleParserCode = fs.readFileSync(path.join(coreDir, 'bible-parser.js'), 'utf8');
const textScannerCode = fs.readFileSync(path.join(coreDir, 'text-scanner.js'), 'utf8');

// Helper for Mock getAssetUrl
window.ACF_CORE.getAssetUrl = (p) => p;

// Execute Scripts in Mock Context
const sandbox = { 
    window, 
    document, 
    Node, 
    NodeFilter, 
    console, 
    RegExp, 
    parseInt, 
    parseFloat, 
    Map 
};
vm.createContext(sandbox);
vm.runInContext(bibleParserCode, sandbox);
vm.runInContext(textScannerCode, sandbox);

// Load Mapping
const mappingPath = path.join(rootDir, 'data', 'mappings', 'bible_mapping.json');
const mapping = JSON.parse(fs.readFileSync(mappingPath, 'utf8'));


// --- 3. TEST SETUP ---
const testText = `Pular para o conteúdo
IRSE – TESTE

Página de exemplo
Blog
O DÍZIMO NO NOVO TESTAMENTO?
Pergunta: — O que é o dízimo no Novo Testamento? Quanto? Devemos dar segundo a Lei ou segundo o amor?

Resposta: — Esta nota é apenas uma breve descrição do que a Bíblia ensina acerca de “dízimo” versus “dar”. Sou grato e recomendo vivamente a obra do Dr. Russell Earl Kelly, Should the Church Teach Tithing? A Theologian’s Conclusions about a Taboo Doctrine, Writers Club Press (11 de janeiro de 2000), para um estudo mais completo deste importantíssimo tema bíblico. Muitos dos pensamentos abaixo advêm do meu diálogo com o Dr. Kelly.

Uma distinção importante precisa ser feita a respeito da “oferta” dos cristãos hoje. Como veremos brevemente abaixo, o dízimo foi ordenado a Israel quando habitava na Terra Prometida. Havia essencialmente três dízimos para Israel. Esta era a Lei da terra. Contudo, hoje a Igreja se expandiu para além dos limites da Terra Prometida. Hoje, a Igreja já não está debaixo da Lei (Romanos 10:4; Gálatas 3:23 – 25; Efésios 2:15; Colossenses 2:14). Hoje, o cristão deve “dar” por amor, não por Lei. Esta distinção é importante de ser mantida — pois faz parte do Evangelho “que uma vez foi entregue aos santos” (Judas 1:3).

Como escreveu certa vez John Owen: — “Permitir-me-ei dizer que não é uma defesa segura para muitos insistirem que os dízimos são devidos e divinos, como dizem, ou seja, por uma Lei vinculante de Deus, agora sob o Evangelho. […] A Lei específica do dízimo não é confirmada no Evangelho […] é impossível que uma única regra certa seja prescrita a todas as pessoas[2]”.

1 – Fatos sobre o dízimo no Antigo Testamento.

Consideremos, por um momento, alguns fatos elementares a respeito do “dízimo” no Antigo Testamento:

[1] – Dízimo agrícola – O dízimo do Antigo Testamento recaía sobre a “terra” (baseado em alimentos), não sobre “cada indivíduo”. Ele incidia sobre o fruto das árvores frutíferas e das oliveiras, sobre o aumento dos campos e dos rebanhos (Levítico 27:30 – 33[3]; Números 18:21 – 24[4]; Deuteronômio 14:22 – 29[5]). Dos rebanhos, apenas o décimo animal que passava sob a vara do pastor era consagrado (Levítico 27:32); se alguém possuísse apenas nove animais, nenhum dízimo era requerido — não havia dízimo de animal. Não havia exigência escriturística de dar sobre o aumento que não fosse agrícola. Contudo, havia um dízimo “voluntário” em relação ao despojo de guerra, o que encontramos no dízimo de Abrão a Melquisedeque. Este dízimo foi: — [1] – “voluntário” e [2] – “um voto” (Abrão o ofereceu porque havia feito um voto – Gênesis 14:22 – 24[6]). Mas a Bíblia não registra se Abrão deu dízimos antes ou depois desse episódio com Melquisedeque. Embora seja verdade que Abraão ofereceu um dízimo voluntário antes da instituição da Lei (Gênesis 14:20[7]), também devemos notar que Abraão foi circuncidado antes da Lei (Gênesis 17:10). A circuncisão não é uma exigência para os crentes hoje. De fato, Paulo declara especificamente que isso não faz parte do nosso relacionamento com Deus (Gálatas 5:11, 12). Note-se, ainda, que Jacó também fez um voto voluntário de dízimo (Gênesis 28:22).

[2] – O aumento da terra – O dízimo no Antigo Testamento recaía sobre o aumento proveniente da terra, demonstrando a provisão de Deus e o seu senhorio sobre a Terra Prometida (Levítico 27:30 – 33; Números 18:21 – 24; Deuteronômio 14:22 – 29). “Guarda-te, que não desampares ao levita todos os teus dias na terra” (Deuteronômio 12:19). A Igreja, porém, já não habita naquela terra. Ela se expande continuamente a toda tribo e nação (Apocalipse 5:9; 7:9). E, por certo, os levitas já não existem.

Os rabinos judeus, em nossos dias, recorrem a um sistema de patronato — estabelecendo certa quantia em dinheiro para cada assento nas sinagogas — a fim de levantar fundos, já que não podem mais receber dízimos para o sustento de um Templo e de levitas que não mais existem (ao menos, conforme a sua concepção acerca da existência do Templo). Tal prática evidencia o quão estreitamente associam o dízimo do Antigo Testamento à Terra Prometida.

A maioria dos cristãos da Nova Aliança vive fora da Terra Prometida (cf. Deuteronômio 12:19). Embora não se refira à Lei moral, os cristãos da Nova Aliança estão isentos da Lei do Antigo Testamento (Romanos 2:12; Gálatas 3:23, 24; 4:5; cf. Romanos 7:1) — Lei esta que Cristo cumpriu (Mateus 5:17 – 20; Atos 13:29). Portanto, não necessitam “dizimar” (Deuteronômio 12:19).

[3] – Três dízimos no Antigo Testamento – Não havia apenas um dízimo no Antigo Testamento, mas três: — [A] – um destinado aos levitas e sacerdotes; [B] – outro a ser consumido na festa dos Tabernáculos; e [C] – outro, a cada terceiro ano, para o socorro [alívio] dos pobres[8]. Se o dízimo ainda é obrigatório, não deveria a Igreja do Novo Testamento pagar “três” dízimos? Se alguém deseja argumentar pela “continuidade” do dízimo do Antigo Testamento, então todos os três dízimos precisam ser exigidos — não apenas um dízimo de 10%. Há, pois, continuidade ou não há? Se alguém deseja colocar-se debaixo da Lei, então precisa obedecer a toda a Lei (Romanos 7:1).

[4] – Sétimo ano — sem dízimo no Antigo Testamento – Como a terra deveria permanecer em repouso e as árvores sem colheita a cada sétimo ano, não havia dízimo do Antigo Testamento proveniente da terra nesse ano (Êxodo 23:11; Levítico 25:11, 12). Se desejamos enfatizar a continuidade da Lei, então precisamos perguntar: — “As Igrejas do Novo Testamento que ensinam um dízimo neotestamentário observam a suspensão do sétimo ano?”. Se não, por que não? Veja o item 3 acima[9].

[5] – O dízimo dos Levitas não mais existe – O dízimo do Antigo Testamento era destinado aos levitas. Não era para a manutenção de edifícios ou programas; estes provinham das “ofertas”. Assim, dízimos e ofertas eram distintos. Os levitas, por sua vez, separavam o dízimo daquilo que recebiam, entregando-o aos sacerdotes arônicos. Isso servia para repassar o incremento agrícola que lhes era dado. Os levitas não podem mais receber dízimos – sua linhagem não existe mais. “Guarda-te, que não desampares ao levita todos os teus dias na terra” (Deuteronômio 12:19).

[6] – Nenhum dízimo sobre salários – As Escrituras do Antigo e do Novo Testamento não dizem nada sobre um dízimo sobre salários, renda de comércio ou investimentos, nem sobre os produtos da terra “fora” da Terra Prometida. Gade estava fora da Terra Prometida propriamente dita, mas a região tribal de Gade fazia parte de sua herança prometida por Deus. Assim, em essência, eles estavam em sua Terra Prometida. Consequentemente, vemos que não havia um dízimo obrigatório fora da Terra Prometida. Compare com o item 1 acima[10].

Não havia no Antigo Testamento nenhuma lei que exigisse um dízimo, muito menos três dízimos, sobre o salário de alguém. Embora mencionados nas Escrituras do Antigo Testamento, os seguintes grupos não receberam instruções explícitas sobre a obrigatoriedade de dizimar: — [1] pescadores (Levítico 11:9 – 12), [2] aqueles envolvidos na mineração (Deuteronômio 8:9), [3] os que atuavam no comércio de madeira (1 Reis 5:7 – 12) e [4] os trabalhadores da construção (1 Reis 5:13 – 18).

[7] – Nenhum dízimo para os pobres – O Antigo Testamento nunca determinou que os pobres fossem obrigados a “dizimar”. Embora pudessem oferecer “ofertas” voluntárias, não havia exigência de dízimo para eles (Levítico 5:11 – 13[11]; 14:21[12]; 27:8[13]). Pelo contrário, os pobres eram beneficiários dos dízimos, das ofertas, das sobras da colheita e da abundância de Israel (Deuteronômio 26:12, 13; Malaquias 3:5; cf. Levítico 25:6, 8 – 15, 25:16 – 25; Números 36:4; Deuteronômio 24:19 – 21; Ester 9:22; Ezequiel 46:17).

Robert Spender, no “Baker’s Evangelical Dictionary of OT Theology”, afirma: — “Além das leis diretas, diversas instituições do Antigo Testamento incluíam disposições especiais para os pobres. As leis de respigas eram destinadas às viúvas, aos órfãos, aos estrangeiros e aos pobres (Levítico 19:9, 10; 23:22; Deuteronômio 24:19 – 22). Durante o ano sabático, as dívidas deveriam ser canceladas (Deuteronômio 15:1 – 9), e o Jubileu proporcionava a libertação de hebreus que haviam se tornado servos devido à pobreza (Levítico 25:39 – 41; 25:54). Durante esses períodos festivos, os pobres tinham livre acesso aos produtos de todos os campos (Êxodo 23:11; Levítico 25:6, 7, 12)”.

Outras estipulações para auxiliar os pobres incluíam o direito de redenção da escravidão por um parente consanguíneo (Levítico 25:47 – 49), o sustento proveniente do dízimo trienal (Deuteronômio 14:28, 29) e disposições especiais relativas às ofertas pelo pecado. Esta última Lei ilustra a natureza relativa do conceito de pobreza. Se alguém não pudesse arcar com o cordeiro usual para a expiação, poderia trazer dois pombos (Levítico 5:7); mas, para aquele que não pudesse custear nem mesmo dois pombos, havia a possibilidade de oferecer um décimo de efa (ou efá) de farinha (Levítico 5:11). Claramente, a Lei enfatizava que a pobreza não era motivo para exclusão da expiação e do culto.

D. James Kennedy escreveu: — “Os pobres não têm a obrigação de oferecer dízimos, mas sim de recebê-los, seja diretamente de vizinhos e amigos compassivos, seja por meio do ministério dos clérigos. Qualquer contribuição oferecida por uma pessoa pobre seria uma oferta voluntária, não um dízimo. O dízimo é o tributo de Deus, exigido daqueles que obtêm lucro de seu trabalho, e não daqueles que dependem de assistência social ou vivem de suas reservas financeiras. Nosso principal dever econômico é garantir alimentos, vestimentas e moradia essenciais para nossas famílias. O dízimo não foi estabelecido para impedir a provisão de suporte material essencial aos membros de nosso lar (1 Timóteo 5:8; Mateus 15:3 – 9)”.

[8] – Dízimos da casa do tesouro – O SENHOR ordenou aos israelitas: — “Trazei todos os dízimos à casa do tesouro, para que haja mantimento na minha casa […]” (Malaquias 3:8 – 12). Este texto é da Lei do Antigo Testamento (cf. Levítico 6:14 – 23). Malaquias 3:8 usa o plural “dízimos” (מַעַשְׂרוֹת), não o singular (הַֽמַּעֲשֵׂ֖ר). Ensinar um único dízimo de 10% a partir deste texto seria um erro, visto que “dízimos” aqui equivalem a um mínimo de 23,33%. O depósito era onde se guardava o dízimo “agrícola” no Antigo Testamento. Note, novamente, a ênfase do dízimo no aumento agrícola. Um minerador não poderia colocar nada no depósito. Além disso, a maioria dos crentes não está mais na Terra Prometida, não possui fazendas e o Templo não está mais de pé.

Quando Jesus deu a Grande Comissão (Mateus 28:18 – 20) à Igreja, este mandato do “depósito” não poderia mais estar em vigor para “toda tribo, língua, povo e nação” (Apocalipse 5:9; 7:9).

Sujeitar os cristãos do Novo Pacto a uma aplicação literal, palavra por palavra, deste texto hoje seria provocar a Deus (Atos 15:10; Gálatas 5:3) e privar os crentes da liberdade que possuem em Cristo (Romanos 8:15; Gálatas 2:4; 5:1, 4, 5; 2 Coríntios 3:17). Utilizar este texto para impor o dízimo no Novo Testamento é desvirtuar as Escrituras de seu sentido original, incorrendo, assim, na pregação de um outro evangelho (Gálatas 1:9).

2 – Fatos sobre o Dízimo no Novo Testamento.

Examinemos brevemente alguns fatos relacionados à menção do “dízimo” no Novo Testamento:

[1] – Mateus 23:23 – Jesus se dirige aos fariseus sobre o dízimo “da hortelã, do endro e do cominho”, plantas de especiarias cultivadas na Terra Prometida. Esses fariseus estavam sob a obrigação dos três dízimos do Antigo Testamento. A pergunta que surge é: — Jesus estava sugerindo que a Igreja de hoje deve superar esses dízimos para alcançar a justiça? Não, o objetivo de Jesus era mostrar que a oferta legalista dos fariseus não resultava em verdadeira justiça. Eles davam os dízimos sem um coração reto. Se o fizessem, estariam também praticando “o juízo, a misericórdia e a fé”. Observe que todos os “ais” pronunciados por Jesus em Mateus 23 foram dirigidos aos fariseus e escribas (Mateus 23:13 – 16, 23, 25, 27, 29), que estavam sob a Lei do Antigo Testamento. Eles eram os que enfatizavam a observância ritual, mas negligenciavam seu dever moral para com o próximo. O que Jesus defende? Lucas afirma: — “Antes dai esmola do que tiverdes, e eis que tudo vos será limpo” (11:41). No Antigo Testamento, uma quantidade obrigatória é mencionada (dízimo, 10%), mas aqui o texto não especifica nenhuma quantia. Em vez disso, o mandamento é simplesmente “dar”. A lei do dízimo teve validade apenas até a cruz. A instrução de Jesus em Mateus 23:23 não justifica que os cristãos continuem obrigados a seguir a lei do dízimo do Antigo Testamento, muito menos os “três” dízimos. Fazer isso seria uma violação da Carta da Liberdade mencionada na Epístola aos Gálatas (Gálatas 5:1, 2; cf. Gálatas 2:4). A questão central em Gálatas era que os gentios estavam sendo instruídos a não apenas crer em Jesus Cristo para a salvação, mas também a aceitar a circuncisão e, consequentemente, comprometer-se a seguir a lei judaica como um caminho para a salvação (Gálatas 2:3 – 5, 12; 4:10; 5:3 – 6; 6:12, 13; cf. Colossenses 2:16 – 20; Levítico 23:2; 1 Crônicas 23:31; 2 Crônicas 31:3; Neemias 10:33 etc.). No entanto, isso é um evangelho falsificado, que na verdade não é Evangelho, e por isso era e é fatal (Gálatas 1:6, 7). O autor Pipa afirma: — “Adicionar algo ao Evangelho — seja obras, sacramentos, batismo ou qualquer outra coisa — é diminuir o Evangelho”. Várias nuances dessa “filosofia” (um evangelho falso) eram comuns entre os primeiros cristãos judeus (Atos 15:1; 21:20, 21; Filipenses 3:2, 3; Colossenses 2:8 – 23). Aqueles que viviam sob a Lei (os judeus) foram libertos para algo muito melhor, e o mesmo vale para a Igreja hoje. Em Romanos 7:6, Paulo escreve: — “Mas agora temos sido libertados da Lei, tendo morrido para aquilo em que estávamos retidos; para que sirvamos em novidade de espírito, e não na velhice da letra”. O cristão que tenta seguir a Lei para ser justificado ou santificado caiu da graça (Gálatas 5:4). Atos 2:42 afirma que a Igreja primitiva seguia não as regras do Antigo Testamento, mas a “doutrina dos Apóstolos”. O dízimo foi instituído quando havia uma necessidade: — o estabelecimento do sacerdócio. Mas ele foi dissolvido quando um sacerdócio superior chegou (Hebreus 8:1 — 10:18). As leis cerimoniais — como os holocaustos, as ofertas de cereais, as regras alimentares, as purificações rituais, as leis sobre lepra e escravidão, as festas e o ano do Jubileu — não se aplicam mais aos crentes.

[2] – Marcos 14:41 – 44 – A mulher viúva que deu tudo (Marcos 14:41 – 44). Ela deu uma oferta voluntária, não um dízimo obrigatório do Antigo ou Novo Testamento. Note, ela ainda estava operando sob a Lei do Antigo Testamento (Veja o item 3 abaixo). Embora se possa ensinar a “doação pela graça” a partir deste texto, se faz um mau uso do texto se tentar ensinar um “dízimo obrigatório” a partir dele! No entanto, devemos ser doadores agradecidos. Nosso desejo deve ser dar tudo o que temos. Mas isso não deve ser o “mandato” da Igreja.

[3] – Lucas 18:12 – É frequentemente usado para dizer que um cristão hoje deveria dizimar “tudo quanto [possui]” (Lucas 18:12). No entanto, sabemos, ao estudar o Antigo Testamento, que a Lei nunca exigiu um dízimo sobre “tudo quanto [possui]”, mas apenas sobre itens agrícolas específicos em Israel (Levítico 27:30 – 33; Números 18:21 – 25; Deuteronômio 14:22 – 29; cf. Deuteronômio 12:19). Jesus contou a parábola em Lucas 18 como: — [A] – um antídoto para a autojustiça por parte dos fariseus e [B] – a culpa não merecida por parte das pessoas comuns da época de Jesus que sentiam que nunca poderiam ser santas. Portanto, o propósito desta parábola era libertar ambas as pessoas de tal escravidão. Você está escravizado hoje?

[4] – Hebreus 7:9 – O fato de Abraão ter sido abençoado por Melquisedeque e ter dado a ele os dízimos ilustra a superioridade de Melquisedeque e, ainda mais, de Cristo sobre o sacerdócio levítico (Hebreus 7:1 – 10). No entanto, o texto prossegue observando que quando o sacerdócio é mudado, a Lei também é! A Lei permaneceu a mesma? Não (Hebreus 7:12). Se houve uma mudança no sistema cerimonial levítico, isso afeta o dízimo. As leis que ordenavam que os dízimos fossem dados aos levitas são obsoletas (Deuteronômio 12:19), a menos que sejam restabelecidas em outro lugar no Novo Testamento. Além disso, deve-se reiterar que o dízimo de Abraão foi, ao mesmo tempo, voluntário e um voto. Em Gênesis 14:21 – 24, fica claro que não se tratava de um dízimo obrigatório de 10%. Podemos mostrar no Novo Testamento onde o dízimo obrigatório (seja os três, ou até mesmo um) foi restabelecido? Não! No entanto, o Novo Testamento nos mostra que a pessoa deve “dar” com alegria (Romanos 15:25 – 27; 1 Coríntios 16:2; 2 Coríntios 8 etc.), mas não “dizimar” (seja os três ou até mesmo um).

[5] – A reconstrução do verdadeiro Templo – O Templo foi destruído em 70 d.C. Contudo, hoje estamos testemunhando uma empolgante “reconstrução” do Templo corporal (1 Coríntios 3:9, 16, 17; 6:19; 2 Coríntios 6:16; Efésios 2:21, 22; 1 Timóteo 3:15; Hebreus 3:6). Simplificando, o Templo que está sendo construído hoje é a Igreja. À medida que o Templo da Nova Aliança é edificado, ele deve ser sustentado da mesma forma que a reconstrução do Templo no Antigo Testamento — com ofertas voluntárias e espontâneas (cf. Esdras 1:4, 6; 7:16; 8:25).

[6] – Dízimos por voto — É claro que havia várias ofertas devidas a votos feitos. Dependendo do voto, estas podem ser obrigatórias. Se uma pessoa faz um voto de dizimar 10% pelo resto da vida, então ela é obrigada a dar 10% até a morte etc. Contudo, Deus nos instrui a não sermos precipitados com nossos votos (Eclesiastes 5:1 – 7). Com as drásticas mudanças em nossa economia, este é um conselho sábio a ser seguido. Observamos novamente que Abraão é registrado por ter feito apenas “um” voto deste tipo. Notamos também que Jacó fez um voto de dízimo voluntário (Gênesis 28:22). Embora nosso desejo deva ser dar 100%, isso seria antibíblico na prática, pois negligenciar a família não é bíblico (cf. 1 Timóteo 5:8).

[7] – Jesus não deu um dízimo obrigatório — Não se diz que Jesus tenha “dizimado” (Mateus 12:1, 2; Marcos 2:23, 24; Lucas 6:1, 2). Mateus 12:1, 2 diz: — “Naquele tempo passou Jesus pelas searas, em um sábado; e os seus discípulos, tendo fome, começaram a colher espigas, e a comer. E os fariseus, vendo isto, disseram-lhe: — ‘Eis que os teus discípulos fazem o que não é lícito fazer num sábado’”.

Embora Jesus tenha cumprido toda a Lei (Mateus 5:17 – 20; Romanos 10:4) e tenha dado a Deus o que era de Deus (Mateus 22:20 – 22), Ele era carpinteiro e, portanto, não estava envolvido com a produção agrícola (da terra, aumento de campos, vinhas, pomares, rebanhos e colmeias). Por isso, Ele não era obrigado a dar um “dízimo” normal. Nenhum dos discípulos era fazendeiro ou pastor e, portanto, eles também não eram obrigados a pagar dízimos. Todos davam ofertas voluntárias (por serem pobres). Eles obedeciam à Lei ao não dizimar! Neste incidente, eles estavam praticando a Lei da respiga[14], que era especificamente para os pobres (Levítico 19:9, 10; 23:22; Deuteronômio 24:19; Rute 2:2, 15). Os fariseus não repreenderam Jesus e seus discípulos por respigar. Eles não repreenderam Jesus e seus discípulos por não pagarem um dízimo em sua colheita! A única acusação é que eles realizaram trabalho no dia de sábado, o que Jesus corrigiu os fariseus (Mateus 12:3 – 8). Jesus de fato “deu” aos pobres, mas uma porcentagem nunca é mencionada (João 12:4 – 6; Provérbios 14:31; 28:27; Tiago 1:27; 1 João 3:17, 18). Jesus alimentou os pobres etc. (Mateus 14:15 – 21; Lucas 9:12 – 15). Ele “deu” perfeitamente e sem pecado (Mateus 5:48), mas nunca foi obrigado a “dizimar”.

[8] – Alguns pontos de resumo – Deus prescreveu divinamente termos para o dízimo que não podem e não devem ser aplicados hoje:

[A] – A maioria da Igreja não vive na Terra Prometida.

[B] – A tribo levítica não continua.

[C] – Não existe um Templo central.

[D] – A maioria dos crentes não tem aumento agrícola da Terra Prometida.

[E] – Hebreus 8 descontinuou o dízimo obrigatório, pois o sacerdócio levítico não existe mais e um sacerdócio maior agora existe.

[F] – Não há dízimo sobre salário, remuneração, ou “tudo o que tenho” etc., nas Escrituras.

[G] – Os pobres não eram obrigados a dizimar.

Agora, o resto da história.

No entanto, embora o dízimo da Antiga Aliança não seja mais aplicável, podemos e devemos aprender com ele. O cristão deve dar o melhor de seu aumento, generosamente, apoiando aqueles que pregam e ensinam a Palavra. Além disso, devem ajudar o pobre, a viúva e o órfão. No entanto, isto não é um “dízimo obrigatório”. A Nova Aliança fala sobre “dar”, não sobre “dizimar” obrigatoriamente.

Alguns princípios de “doação” (não dízimo) do Novo Testamento.

O princípio e a liberdade do “dar” (não do dizimar) no Novo Testamento são o ato de “doar” de forma sacrificial, alegre, voluntária e proporcional para o sustento do ministério da Palavra e o auxílio aos necessitados, além da gestão adequada de todo o resto como pertencente a Deus. Veja Mateus 6:2 – 4; 25:34 – 46; Marcos 4:24; Lucas 6:38; 12:15, 34; 20:25; 21:1 – 4; Atos 2:44, 45; Romanos 15:25 – 27; 1 Coríntios 16:2; 2 Coríntios 8:2 – 5; 9:6 – 12; 1 Timóteo 6:17 – 19; Hebreus 13:16; 1 Pedro 4:10, 11 etc. Assim, o cristão deve dar voluntariamente por amor, não por Lei. Ele deve dar:

[A] – Do seu aumento, conforme o Senhor supre e direciona.

[B] – Como sua primeira prioridade, determinada de antemão, não como uma reflexão tardia do que sobrou. O cristão não deve perguntar: — “Certo, quanto eu tenho que dar este mês?”, mas sim, com alegria avassaladora e entusiasmo: — “Quanto eu posso dar? Posso dar ainda mais este mês?” etc. Louvado seja Deus!

[C] – Do seu melhor.

[D] – Sacrificialmente! Sacrificialmente! Sacrificialmente!

[E] – Alegremente, jubilosamente e de modo entusiasmado.

[F] – Para prover a pregação e o ensino da Palavra, o envio de pregadores e o alívio dos que sofrem; primeiro os crentes, depois os outros.

[G] – Por meio dos presbíteros e diáconos (cuidando dos pobres, das viúvas e dos órfãos) da Igreja.

[H] – Cumprindo quaisquer promessas ou votos que fizer de acrescentar algo ao reino de Deus.

[I] – Como um privilégio da graça, não como um fardo.

[J] – Por amor a Cristo, ao seu Reino, ao seu povo e ao próximo.

No entanto, a “doação” do Novo Testamento não é um dízimo obrigatório de 10%. Pode-se procurar, mas não se encontra este mandamento de dizimar nas Escrituras do Novo Testamento! Até mesmo o quarto voto de membro da PCA não exige um dízimo. Ele diz: — “Você promete apoiar a Igreja em sua adoração e trabalho da melhor maneira possível?”. Note que isso não diz “dizimar” (um 10% obrigatório), mas “apoiar […] da melhor maneira possível”. “Cada um esteja inteiramente seguro em sua própria mente” (Romanos 14:5).

Observe que o BCO 54–1 da PCA menciona um “dízimo”. No entanto, o BCO 54–1 não é constitucionalmente vinculativo. Note que ele também difere, em substância, das deliberações presbiterianas anteriores: — [1] – a referência da Assembleia de 1854 ao dízimo como “presunção” e [2] – a linguagem de 1933 de “porção digna”. É interessante que a Confissão de Fé de Westminster (21:5), como originalmente escrita, não contém nenhuma referência a coletas como um elemento ordinário de adoração ou como um elemento ocasional de adoração.

Que maravilhoso presente — A doação.

Temos o “privilégio” e a “liberdade” de “dar”. Louvado seja o Senhor! Aproveite esta oportunidade para considerar, em oração, com alegria e sacrificialmente, o que o Senhor gostaria que você desse hoje – e todos os dias – à sua Igreja, aos pobres e a outros. Calvino escreveu: — “Ai da nossa indolência! — que se manifesta nisto, que enquanto Deus nos convida com tanta bondade à honra do sacerdócio, e até mesmo coloca sacrifícios em nossas mãos, nós, no entanto, não sacrificamos a Ele […]. Pois os altares, nos quais os sacrifícios de nossos recursos deveriam ser apresentados, são os pobres e os servos de Cristo. Negligenciando estes, alguns esbanjam seus recursos em todo tipo de luxo, outros no paladar, outros em trajes imodestos, outros em moradias magníficas[15]”.

Deus não nos limita a uma lei de meros 10% ou 23,3%; em vez disso, como Ele nos deu abundantemente, agora temos a oportunidade de devolver o que já é dEle (Salmos 50:10, 12; 89:11; 1 Coríntios 10:26; cf. Êxodo 9:29; Deuteronômio 10:14) de acordo com seu Espírito. No entanto, Ele entende a situação dos pobres, da viúva e do órfão. Ele provê para eles através de você e de mim (Êxodo 23:11; Levítico 19:10; 23:22; Deuteronômio 15:9 etc.). Em sua providência, Deus até provê para os pobres por meios miraculosos em alguns momentos (1 Reis 17:7 – 24; Mateus 14:13 – 21; Marcos 6:31 – 44; Lucas 9:10 – 17; João 6:5 – 15). E até mesmo os pobres podem ser tocados pelo Espírito para dar uma oferta voluntária de algum tipo. Às vezes, eles são movidos pelo Espírito Santo a serem os mais generosos de todos os doadores (Marcos 12:41 – 44; Lucas 21:1 – 4). Oh, a maravilhosa obra do Espírito de Deus e seu dom de dar! Como você exercerá este dom, esta semana, este mês e durante todo o ano?

“Mais bem–aventurada coisa é dar do que receber” (Atos 20:35; cf. 2 Coríntios 9:6, 7).

Referências históricas.
`;

// Insert text into mock body
window.document.body.textContent = testText;

// Run Scanner
window.ACF_CORE.autoScanAndLink(mapping, 'underline', 'click');

// Collect Results
const underlines = [];
const treeWalker = window.document.createTreeWalker(
    window.document.body,
    NodeFilter.SHOW_TEXT,
    { acceptNode: n => n.parentNode.classList.contains('acf-ref-underline') ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_SKIP }
);

// Our mock TreeWalker is simple and doesn't handle FILTER_SKIP perfectly in the walker function 
// (it iterates all and filters). But traverse function only pushed ACCEPT nodes.
// But we need to traverse ELEMENT nodes to find spans.
// My mock TreeWalker only traversed text nodes that passed filter.
// If the text node is inside span.acf-ref-underline, the parent check works.
// So let's see.

// Actually, let's just querySelectorAll since we are in a mock environment (and I mocked Element/Text traversal slightly wrong for complex DOM).
// But I implemented querySelector for 'body' only.
// I will traverse manually.

function collectUnderlines(node) {
    if (node.nodeType === Node.ELEMENT_NODE) {
        if (node.classList.contains('acf-ref-underline')) {
            underlines.push(node.textContent);
        }
        node.childNodes.forEach(collectUnderlines);
    }
}

collectUnderlines(window.document.body);

console.log(`\n=== INTEGRATION TEST RESULTS (${underlines.length} matches) ===\n`);
underlines.forEach((u, i) => console.log(`[${i+1}] "${u}"`));

// --- ASSERTIONS ---
// We use the same 'mustMatch' list as before (omitted for brevity in this tool call, but concept is same)
// I will just perform specific checks for the reported issues.

const checks = [
    { name: "Trailing comma removal", check: u => u.includes("Gênesis 14:21") && !u.includes("Gênesis 14:21 – 24,") },
    { name: "Complex list matching (5:1, 4, 5)", check: u => u.includes("Romanos 8:15") && u.includes("5:1, 4, 5") },
    { name: "Colon separator (9:6, 7: -)", check: u => u.includes("2 Coríntios 9:6, 7") },
    { name: "List 1, 2, 3", check: u => u.includes("1, 2, 3") }, // Wait, this wasn't in text.
    // Check specific text content
];

let failed = false;

// Check 1: Gênesis 14:21 – 24
const gen14 = underlines.find(u => u.includes("Gênesis 14:21"));
if (gen14) {
    if (gen14.trim().endsWith(',')) {
        console.error(`❌ FAIL: Trailing comma found in "${gen14}"`);
        failed = true;
    } else {
        console.log(`✅ PASS: Trailing comma removed in "${gen14}"`);
    }
} else {
    console.error(`❌ FAIL: Gênesis 14:21 not found`);
    failed = true;
}

// Check 2: Romanos 8:15...
const rom8 = underlines.find(u => u.includes("Romanos 8:15"));
if (rom8) {
    if (rom8.includes("5:1, 4, 5")) {
        console.log(`✅ PASS: Complex list matched: "${rom8}"`);
    } else {
        console.error(`❌ FAIL: List incomplete. Got: "${rom8}"`);
        failed = true;
    }
} else {
    console.error(`❌ FAIL: Romanos 8:15 not found`);
    failed = true;
}

// Check 3: 2 Coríntios 9:6, 7
const cor9 = underlines.find(u => u.includes("2 Coríntios 9:6"));
if (cor9) {
    if (cor9.includes("9:6, 7")) {
        console.log(`✅ PASS: 2 Coríntios 9:6, 7 matched.`);
    } else {
        console.error(`❌ FAIL: 2 Coríntios 9:6 incomplete. Got: "${cor9}"`);
        failed = true;
    }
} else {
    console.error(`❌ FAIL: 2 Coríntios 9:6 not found`);
    failed = true;
}

if (failed) process.exit(1);
console.log("\n🎉 INTEGRATION TEST PASSED");
