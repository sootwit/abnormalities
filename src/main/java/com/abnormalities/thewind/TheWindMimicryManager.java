package com.abnormalities.thewind;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TheWindMimicryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|TheWind|Mimicry");
    private static long lastChat = 0;
    private static long lastMimic = 0;
    private static final java.util.Random RNG = new java.util.Random();

    private static final Map<String, List<String>> LURE_MESSAGES = new HashMap<>();
    private static final Map<String, List<String>> AMBIENT_MESSAGES = new HashMap<>();
    private static final Map<String, List<String>> THREAT_MESSAGES = new HashMap<>();
    private static final Map<String, List<String>> CONFUSION_MESSAGES = new HashMap<>();

    static {
        LURE_MESSAGES.put("en_us", List.of(
                "bro i found diamonds at X Y Z come check",
                "ayo theres a chest here with good loot",
                "come look at this structure i found",
                "is that a village over there?",
                "trust me theres nothing here come see",
                "i left something for you at X Y Z",
                "bro theres a spawner here come quick",
                "you wont believe what i found at X Y Z",
                "come to X Y Z theres something crazy",
                "i think i found a stronghold entrance",
                "theres a chest in this wall come look",
                "bro theres diamonds behind this wall",
                "ayo come check this cave its insane",
                "i found an end city right here",
                "theres a fortress at X Y Z come",
                "you gotta see this trust me",
                "i found a woodland mansion nearby",
                "theres a pillager outpost at X Y Z",
                "come here theres a secret room",
                "bro theres a vault here open it",
                "i left good stuff at X Y Z for you",
                "ayo theres a dungeon under here",
                "come check this out its worth it",
                "trust me you want to be at X Y Z",
                "theres something cool at these coords",
                "i found a buried treasure at X Y Z",
                "bro theres a shipwreck here come",
                "ayo come see this its wild",
                "i found an ancient city entrance",
                "theres a trial chamber at X Y Z",
                "come here ill show you something",
                "bro trust me theres good loot here",
                "i found a trail ruins at X Y Z",
                "ayo come to these coords quick",
                "theres something you need to see",
                "i left you something special at X Y Z",
                "come check this cave system out",
                "bro theres a mineshaft under here",
                "ayo i found a portal to the end",
                "trust me these coords are worth it",
                "i found a desert temple at X Y Z",
                "theres a jungle temple nearby come",
                "come here theres an igloo basement",
                "bro i found a ocean monument",
                "ayo come to X Y Z right now",
                "theres something incredible at these coords",
                "i found awitch hut at X Y Z",
                "come check this out its amazing",
                "bro trust me come to X Y Z",
                "i found something you need to see",
                "ayo these coords have great loot"
        ));
        LURE_MESSAGES.put("tr_tr", List.of(
                "kanka X Y Z de elmas buldum gel bak",
                "abi burada sandik var icinde iyi seyler var",
                "gel bi bak su yapiyi buldum",
                "sence o koy mu orada mi?",
                "guvende bana hicbişe yok gel gor",
                "X Y Z ye bir sey biraktim sana",
                "kanka burada spawner var cok hizli gel",
                "inanamazsin X Y Z de ne buldum",
                "gel su koordinatlara cok garip bir sey var",
                "sanki stronghould girisi buldum",
                "bu duvarda sandik var gel bak",
                "kanka duvarin arkasinda elmas var",
                "abi bu magaraya bak cok guzel",
                "end city buldum tam burada",
                "X Y Z de fortress var gel",
                "guvende buraya gel cok havalı",
                "woodland mansion buldum yakinda",
                "X Y Z de pillager outpost var",
                "gel burada gizli oda var",
                "kanka burada vault var ac bakalim",
                "sana X Y Z de iyi seyler biraktim",
                "abi burada dungeon var altta",
                "gel bak buna deger",
                "guvende bu koordinatlara gel",
                "burada garip bir sey var",
                "X Y Z de gömülü hazine buldum",
                "kanka burada shipwreck var gel",
                "abi gel bak cok garip",
                "ancient city girisi buldum",
                "X Y Z de trial chamber var",
                "gel buraya gosteriyim sana",
                "kanka guvende iyi loot var",
                "trail ruins buldum X Y Z de",
                "abi hemen su koordinatlara gel",
                "burada gormen gereken bir sey var",
                "sana ozel bir sey biraktim X Y Z de",
                "gel bu magara sistemine bak",
                "kanka altta mineshaft var",
                "abi end portal buldum",
                "guvende bu koordinatlar deger",
                "X Y Z de desert temple buldum",
                "yakinda jungle temple var gel",
                "gel burada igloo bodrum katı var",
                "kanka ocean monument buldum",
                "abi hemen X Y Z ye gel",
                "bu koordinatlarda inanilmaz seyler var",
                "X Y Z de witch hut buldum",
                "gel bak buna cok havalı",
                "kanka guvende X Y Z ye gel",
                "bir sey buldum gormen lazim",
                "abi bu koordinatlarda great loot var"
        ));
        LURE_MESSAGES.put("es_es", List.of(
                "hermano encontre diamantes en X Y Z ven a ver",
                "hay un cofre aqui con cosas buenas",
                "ven a ver esta estructura que encontre",
                "es eso un pueblo alla?",
                "confia en mi no hay nada aqui ven",
                "deje algo para ti en X Y Z",
                "hermano hay un spawner aqui rapido",
                "no vas a creer lo que encontre en X Y Z",
                "ven a X Y Z hay algo increible",
                "creo que encontre una entrada al stronghold",
                "hay un cofre en este muro ven a ver",
                "hermano hay diamantes detras de este muro",
                "ven a ver esta cueva es genial",
                "encontre una ciudad del fin aqui",
                "hay una fortress en X Y Z ven",
                "confia en mi tienes que ver esto",
                "encontre una mansion del bosque cerca",
                "hay un puesto de saqueadores en X Y Z",
                "ven aqui hay una sala secreta",
                "hermano hay un vault aqui abrela",
                "te deje cosas buenas en X Y Z",
                "hay un dungeon aqui abajo ven",
                "ven a ver esto vale la pena",
                "confia en mi ven a estas coordenadas",
                "hay algo genial en estas coordenadas",
                "encontre un tesoro enterrado en X Y Z",
                "hermano hay un naufragio aqui ven",
                "ven a ver esto es loco",
                "encontre una entrada a la ciudad antigua",
                "hay una camara de prueba en X Y Z",
                "ven aqui te voy a mostrar algo",
                "hermano confia en mi hay buen loot",
                "encontre ruinas de sendero en X Y Z",
                "ven a estas coordenadas rapido",
                "hay algo que necesitas ver",
                "te deje algo especial en X Y Z",
                "ven a ver este sistema de cuevas",
                "hermano hay una mina abandonada aqui abajo",
                "abi encontre un portal al fin",
                "confia en mi estas coordenadas valen la pena",
                "encontre un templo del desierto en X Y Z",
                "hay un templo de la selva cerca ven",
                "ven aqui hay un sótano de iglu",
                "hermano encontre un monumento oceánico",
                "ven a X Y Z ahora mismo",
                "hay algo increible en estas coordenadas",
                "encontre una choza de bruja en X Y Z",
                "ven a ver esto es asombroso",
                "hermano confia en mi ven a X Y Z",
                "encontre algo que necesitas ver",
                "estas coordenadas tienen buen loot"
        ));
        LURE_MESSAGES.put("fr_fr", List.of(
                "mec j'ai trouve des diamants a X Y Z viens voir",
                "il y a un coffre ici avec du butin",
                "viens voir cette structure que j'ai trouvee",
                "c'est un village la-bas?",
                "fais-moi confiance il y a rien ici viens",
                "j'ai laisse quelque chose pour toi a X Y Z",
                "mec il y a un spawner ici viens vite",
                "tu vas pas croire ce que j'ai trouve a X Y Z",
                "viens a X Y Z il y a quelque chose de fou",
                "je crois que j'ai trouve une entree au stronghold",
                "il y a un coffre dans ce mur viens voir",
                "mec il y a des diamants derriere ce mur",
                "viens voir cette grotte c'est ouf",
                "j'ai trouve une cité de l'end ici",
                "il y a une forteresse a X Y Z viens",
                "fais-moi confiance tu dois voir ca",
                "j'ai trouve un manoir du bois pres d'ici",
                "il y a un avant-poste de pillards a X Y Z",
                "viens ici il y a une salle secrete",
                "mec il y a un coffre-fort ici ouvre-le",
                "j'ai laisse des trucs bien a X Y Z pour toi",
                "il y a un donjon ici dessous viens",
                "viens voir ca ca vaut le coup",
                "fais-moi confiance viens a ces coordonnees",
                "il y a quelque chose de cool a ces coordonnees",
                "j'ai trouve un tresor enterre a X Y Z",
                "mec il y a un épave ici viens",
                "viens voir c'est dingue",
                "j'ai trouve une entree a la cité ancienne",
                "il y a une chambre d'épreuves a X Y Z",
                "viens ici je vais te montrer quelque chose",
                "mec fais-moi confiance il y a du bon butin",
                "j'ai trouve des ruines de sentier a X Y Z",
                "viens a ces coordonnees vite",
                "il y a quelque chose que tu dois voir",
                "j'ai laisse quelque chose de special a X Y Z",
                "viens voir ce systeme de grottes",
                "mec il y a une mine abandonnee ici dessous",
                "j'ai trouve un portail vers l'end",
                "fais-moi confiance ces coordonnees valent le coup",
                "j'ai trouve un temple du desert a X Y Z",
                "il y un temple de la jungle pres d'ici viens",
                "viens ici il y a un sous-sol d'igloo",
                "mec j'ai trouve un monument oceanique",
                "viens a X Y Z tout de suite",
                "il y a quelque chose d'incroyable a ces coordonnees",
                "j'ai trouve une cabane de sorciere a X Y Z",
                "viens voir c'est incroyable",
                "mec fais-moi confiance viens a X Y Z",
                "j'ai trouve quelque chose que tu dois voir",
                "ces coordonnees ont du bon butin"
        ));
        LURE_MESSAGES.put("de_de", List.of(
                "bruder ich habe diamanten gefunden bei X Y Z komm schau",
                "hier ist eine trunk mit guter beute",
                "komm schau dir diese struktur an die ich gefunden habe",
                "ist das ein dorf da drueben?",
                "vertrau mir hier ist nichts komm und schau",
                "ich habe etwas fuer dich gelassen bei X Y Z",
                "bruder hier ist ein spawner komm schnell",
                "du wirst nicht glauben was ich bei X Y Z gefunden habe",
                "komm zu X Y Z hier ist etwas verruecktes",
                "ich glaube ich habe einen stronghold eingang gefunden",
                "hier ist eine trunk in dieser wand komm schau",
                "bruder hinter dieser wand sind diamanten",
                "komm schau dir diese hoehle an sie ist cool",
                "ich habe hier eine end stadt gefunden",
                "hier ist eine festung bei X Y Z komm",
                "vertrau mir du musst dir das ansehen",
                "ich habe ein waldschloss in der naehe gefunden",
                "hier ist ein rueckzugsort bei X Y Z",
                "komm hier ist ein geheimer raum",
                "bruder hier ist eintreschrank oeffne ihn",
                "ich habe gute sachen fuer dich bei X Y Z gelassen",
                "hier ist ein dungeons unten komm",
                "komm und sieh dir das an es lohnt sich",
                "vertrau mir komm zu diesen koordinaten",
                "hier ist etwas cooles bei diesen koordinaten",
                "ich habe einen vergrabenen schatz bei X Y Z gefunden",
                "bruder hier ist ein schiffswrack komm",
                "komm und sieh dir das an es ist verrueckt",
                "ich habe einen eingang zur alten stadt gefunden",
                "hier ist eine pruefungskammer bei X Y Z",
                "komm hier ich zeige dir etwas",
                "bruder vertrau mir hier ist gute beute",
                "ich habe truemmerruinen bei X Y Z gefunden",
                "komm zu diesen koordinaten schnell",
                "hier ist etwas das du sehen musst",
                "ich habe etwas besonderes fuer dich bei X Y Z gelassen",
                "komm und sieh dir dieses hohlensystem an",
                "bruder hier ist ein verlassener minenschacht unten",
                "ich habe ein portal zum ende gefunden",
                "vertrau mir diese koordinaten sind es wert",
                "ich habe einen wuestentempel bei X Y Z gefunden",
                "hier ist ein dschungeltempel in der naehe komm",
                "komm hier ist ein iglo keller",
                "bruder ich habe ein ozeanmonument gefunden",
                "komm zu X Y Z sofort",
                "hier ist etwas unglaubliches bei diesen koordinaten",
                "ich habe eine hexenhutte bei X Y Z gefunden",
                "komm und sieh dir das an es ist erstaunlich",
                "bruder vertrau mir komm zu X Y Z",
                "ich habe etwas gefunden das du sehen musst",
                "diese koordinaten haben gute beute"
        ));
        LURE_MESSAGES.put("pt_br", List.of(
                "mano achei diamante em X Y Z vem ver",
                "tem um bau aqui com coisa boa",
                "vem olhar essa estrutura que achei",
                "aquilo e uma vila la?",
                "confia em mim nao tem nada aqui vem",
                "deixei algo pra voce em X Y Z",
                "mano tem um spawner aqui vem rapido",
                "voce nao vai acreditar no que achei em X Y Z",
                "vem pra X Y Z tem algo insano",
                "acho que achei uma entrada pro stronghold",
                "tem um bau nesse muro vem olhar",
                "mano tem diamante atras desse muro",
                "vem olhar essa caverna e incrivel",
                "achei uma cidade do fim aqui",
                "tem uma fortaleza em X Y Z vem",
                "confia em mim voce precisa ver isso",
                "achei uma mansao do bosque perto",
                "tem um posto de saqueadores em X Y Z",
                "vem aqui tem uma sala secreta",
                "mano tem um baquete aqui abre",
                "deixei coisa boa pra voce em X Y Z",
                "tem um morro aqui embaixo vem",
                "vem ver isso vale a pena",
                "confia em mim vem pra essas coordenadas",
                "tem algo legal nessas coordenadas",
                "achei um tesouro enterrado em X Y Z",
                "mano tem um naufragio aqui vem",
                "vem ver isso e loucura",
                "achei uma entrada da cidade antiga",
                "tem uma camara de prova em X Y Z",
                "vem aqui vou te mostrar algo",
                "mano confia em mim tem loot bom",
                "achei ruinas de trilha em X Y Z",
                "vem pra essas coordenadas rapido",
                "tem algo que voce precisa ver",
                "deixei algo especial pra voce em X Y Z",
                "vem ver esse sistema de cavernas",
                "mano tem uma mina abandonada aqui embaixo",
                "achei um portal pro fim",
                "confia em mim essas coordenadas valem a pena",
                "achei um templo do deserto em X Y Z",
                "tem um templo da selva perto vem",
                "vem aqui tem um porao de iglu",
                "mano achei um monumento oceânico",
                "vem pra X Y Z agora",
                "tem algo incrivel nessas coordenadas",
                "achei uma cabana de bruxa em X Y Z",
                "vem ver isso e incrivel",
                "mano confia em mim vem pra X Y Z",
                "achei algo que voce precisa ver",
                "essas coordenadas tem loot bom"
        ));

        AMBIENT_MESSAGES.put("en_us", List.of(
                "anyone else hear that?",
                "i think im lagging",
                "my game just glitched",
                "did you see that?",
                "something feels wrong",
                "why is it so quiet",
                "i heard something behind me",
                "my screen flickered",
                "did the world just move?",
                "i cant find my way back",
                "something is watching",
                "the music stopped",
                "i feel like im not alone",
                "did you hear footsteps?",
                "my torch went out",
                "its getting darker",
                "i think im being followed",
                "something moved in the corner",
                "the cave sounds wrong",
                "i cant see anything"
        ));
        AMBIENT_MESSAGES.put("tr_tr", List.of(
                "siz de duydunuz mu?",
                "sanirim lag yapiyorum",
                "oyunum az once bozuldu",
                "gordunuz mu o neydi?",
                "bir seyler yanlis",
                "neden bu kadar sessiz",
                "arkamda bir sey duydum",
                "ekranim titredi",
                "dunya az once mi hareket etti?",
                "yolunui bulamiyorum",
                "bir sey bizi izliyor",
                "muzik durdu",
                "yalniz oldugumu hissetmiyorum",
                "adim sesleri duydunuz mu?",
                "meşalem söndü",
                "karanlik artiyor",
                "sanirim biri bizi takip ediyor",
                "kosede bir sey hareket etti",
                "magara tuhaf ses cikariyor",
                "hicbir sey goremiyorum"
        ));
        AMBIENT_MESSAGES.put("es_es", List.of(
                "alguien mas escucho eso?",
                "creo que estoy laggeando",
                "mi juego acaba de fallar",
                "viste eso?",
                "algo se siente mal",
                "por que esta tan silencioso",
                "escuche algo atras mio",
                "mi pantalla parpadeo",
                "el mundo se acaba de mover?",
                "no puedo encontrar el camino de vuelta",
                "algo me esta observando",
                "la musica se detuvo",
                "siento que no estoy solo",
                "escucharon pasos?",
                "mi antorcha se apago",
                "esta oscureciendo",
                "creo que me estan siguiendo",
                "algo se movio en la esquina",
                "la cueva suena mal",
                "no puedo ver nada"
        ));
        AMBIENT_MESSAGES.put("fr_fr", List.of(
                "vous avez aussi entendu ca?",
                "je crois que je lag",
                "mon jeu vient de bugger",
                "vous avez vu ca?",
                "quelque chose ne va pas",
                "pourquoi c'est si silencieux",
                "j'ai entendu quelque chose derriere moi",
                "mon ecran a scintille",
                "le monde vient de bouger?",
                "je retrouve pas mon chemin",
                "quelque chose m'observe",
                "la musique s'est arretee",
                "j'ai l'impression de pas etre seul",
                "vous avez entendu des pas?",
                "ma torche s'est eteinte",
                "ça devient plus sombre",
                "je crois qu'on me suit",
                "quelque chose a bouge dans le coin",
                "la grotte sonne bizarre",
                "je vois rien du tout"
        ));
        AMBIENT_MESSAGES.put("de_de", List.of(
                "habt ihr das auch gehoert?",
                "ich glaube ich lagge",
                "mein Spiel ist gerade abgestuerzt",
                "habt ihr das gesehen?",
                "etwas fuehlt sich falsch an",
                "warum ist es so leise",
                "ich habe etwas hinter mir gehoert",
                "mein Bildschirm hat geflackert",
                "hat sich die Welt gerade bewegt?",
                "ich finde den Weg zurueck nicht",
                "etwas beobachtet mich",
                "die Musik hat aufgehoert",
                "ich fuehle mich nicht allein",
                "habt ihr Schritte gehoert?",
                "meine Fackel ist ausgegangen",
                "es wird dunkler",
                "ich glaube jemand folgt mir",
                "etwas hat sich in der Ecke bewegt",
                "die Hoehle klingt falsch",
                "ich kann nichts sehen"
        ));
        AMBIENT_MESSAGES.put("pt_br", List.of(
                "voces tambem ouviram isso?",
                "acho que to lagando",
                "meu jogo acabou de travar",
                "voces viram isso?",
                "algo ta errado",
                "por que ta tao silencioso",
                "ouvi algo la atras",
                "minha tela piscou",
                "o mundo acabou de se mexer?",
                "nao consigo achar o caminho de volta",
                "algo ta me observando",
                "a musica parou",
                "sinto que nao to sozinho",
                "ouviram passos?",
                "minha tocha apagou",
                "ta ficando mais escuro",
                "acho que to sendo seguido",
                "algo se moveu no canto",
                "a caverna ta com som errado",
                "nao consigo ver nada"
        ));

        THREAT_MESSAGES.put("en_us", List.of(
                "dont go there",
                "something is watching",
                "run",
                "its behind you",
                "dont look",
                "stay still",
                "close your eyes",
                "its coming",
                "you should leave",
                "dont turn around",
                "its too late",
                "you cant escape",
                "stop moving",
                "its almost here",
                "dont breathe"
        ));
        THREAT_MESSAGES.put("tr_tr", List.of(
                "oraya gitme",
                "bir sen bakiyor",
                "kos",
                "senin arkanda",
                "bakma",
                "dur hareket etme",
                "gozlerini kapa",
                "o geliyor",
                "buradan git",
                "donup bakma",
                "cok gec",
                "kacisin yok",
                "hareketi dur",
                "o neredeyse geldi",
                "nefes alma"
        ));
        THREAT_MESSAGES.put("es_es", List.of(
                "no vayas ahi",
                "algo te esta observando",
                "corre",
                "esta detras tuyo",
                "no mires",
                "quedate quieto",
                "cierra los ojos",
                "esta viniendo",
                "deberias irte",
                "no te vuelvas",
                "es demasiado tarde",
                "no puedes escapar",
                "deja de moverte",
                "ya casi llega",
                "no respires"
        ));
        THREAT_MESSAGES.put("fr_fr", List.of(
                "n'y va pas",
                "quelque chose t'observe",
                "cours",
                "c'est derriere toi",
                "regarde pas",
                "reste immobile",
                "ferme les yeux",
                "ca vient",
                "tu devrais partir",
                "te retourne pas",
                "c'est trop tard",
                "tu peux pas t'echapper",
                "arrete de bouger",
                "c'est presque la",
                "respire pas"
        ));
        THREAT_MESSAGES.put("de_de", List.of(
                "geh nicht dorthin",
                "etwas beobachtet dich",
                "renn",
                "es ist hinter dir",
                "schau nicht hin",
                "steh still",
                "schliess die augen",
                "es kommt",
                "du solltest gehen",
                "drehe dich nicht um",
                "es ist zu spaet",
                "du kannst nicht entkommen",
                "hoer auf dich zu bewegen",
                "es ist fast da",
                "atme nicht"
        ));
        THREAT_MESSAGES.put("pt_br", List.of(
                "nao va la",
                "algo ta te observando",
                "corre",
                "ta atras de voce",
                "nao olha",
                "fica parado",
                "fecha os olhos",
                "ta vindo",
                "voce deveria ir embora",
                "nao se vire",
                "ja era tarde demais",
                "voce nao pode fugir",
                "para de se mexer",
                "ja ta quase chegando",
                "nao respira"
        ));

        CONFUSION_MESSAGES.put("en_us", List.of(
                "wait did i say that?",
                "i didnt type that",
                "that wasnt me",
                "my chat is broken",
                "who said that?",
                "i think my game is haunted",
                "why is my name in chat?",
                "i didnt join the game",
                "that message isnt from me",
                "my keyboard did that on its own"
        ));
        CONFUSION_MESSAGES.put("tr_tr", List.of(
                "dur ben mi soledim?",
                "ben yazmadim o mesaji",
                "o ben degildim",
                "sohbetim bozuldu",
                "kim soledi?",
                "sanirim oyunum perili",
                "neden ismim sohbette?",
                "ben oyun katilmadim",
                "o mesaj benden degil",
                "klavyem kendi yazdi"
        ));
        CONFUSION_MESSAGES.put("es_es", List.of(
                "espera yo dije eso?",
                "no escribi eso",
                "eso no fui yo",
                "mi chat esta roto",
                "quien dijo eso?",
                "creo que mi juego esta embrujado",
                "por que mi nombre esta en el chat?",
                "no me uni a la partida",
                "ese mensaje no es mio",
                "mi teclado hizo eso solo"
        ));
        CONFUSION_MESSAGES.put("fr_fr", List.of(
                "attends j'ai dit ca?",
                "j'ai pas ecrit ca",
                "c'etait pas moi",
                "mon chat est casse",
                "qui a dit ca?",
                "je crois que mon jeu est hante",
                "pourquoi mon nom est dans le chat?",
                "j'ai pas rejoint la partie",
                "ce message vient pas de moi",
                "mon clavier a fait ca tout seul"
        ));
        CONFUSION_MESSAGES.put("de_de", List.of(
                "warte habe ich das gesagt?",
                "ich habe das nicht getippt",
                "das war nicht ich",
                "mein Chat ist kaputt",
                "wer hat das gesagt?",
                "ich glaube mein Spiel ist verflucht",
                "warum ist mein Name im Chat?",
                "ich bin dem Spiel nicht beigetreten",
                "diese Nachricht ist nicht von mir",
                "meine Tastatur hat das von alleine gemacht"
        ));
        CONFUSION_MESSAGES.put("pt_br", List.of(
                "pera eu falei isso?",
                "nao digitei isso",
                "nao fui eu",
                "meu chat ta bugado",
                "quem disse isso?",
                "acho que meu jogo ta assombrado",
                "por que meu ta no chat?",
                "nao entrei no jogo",
                "essa mensagem nao e minha",
                "meu teclado fez isso sozinho"
        ));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AbnormalitiesConfig.TW_ENABLED.get()) return;
        if (!AbnormalitiesConfig.TW_MIMICRY_ENABLED.get()) return;
        if (!AbnormalitiesConfig.TW_MIMICRY_LANG_CHAT.get()) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        long now = srv.getTickCount();

        int chatChance = AbnormalitiesConfig.TW_MIMICRY_CHAT_CHANCE.get();
        if (chatChance > 0 && now - lastChat >= 6000 && RNG.nextInt(Math.max(1, 100 / Math.max(1, chatChance))) == 0) {
            lastChat = now;
            triggerLanguageChat(srv);
        }

        if (now - lastMimic >= 12000 && RNG.nextInt(400) == 0) {
            lastMimic = now;
            triggerFakeWhisper(srv);
        }
    }

    private static void triggerLanguageChat(net.minecraft.server.MinecraftServer srv) {
        var players = srv.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        String sourceName;
        if (srv.isSingleplayer()) {
            List<String> friends = getEssentialFriendNames();
            if (!friends.isEmpty()) {
                sourceName = friends.get(RNG.nextInt(friends.size()));
                LOGGER.info("[THE_WIND|Mimicry] Using Essential friend name: {}", sourceName);
            } else {
                sourceName = com.abnormalities.FakeNames.NAMES.get(RNG.nextInt(com.abnormalities.FakeNames.NAMES.size()));
                LOGGER.info("[THE_WIND|Mimicry] No Essential friends, using fake name: {}", sourceName);
            }
        } else {
            ServerPlayer source = players.get(RNG.nextInt(players.size()));
            sourceName = source.getName().getString();
            LOGGER.info("[THE_WIND|Mimicry] Using real player name: {}", sourceName);
        }

        String sourceLang;
        if (srv.isSingleplayer()) {
            sourceLang = players.isEmpty() ? "en_us" : getPlayerLanguage(players.get(0));
        } else {
            sourceLang = "en_us";
            for (ServerPlayer p : players) {
                if (p.getName().getString().equals(sourceName)) {
                    sourceLang = getPlayerLanguage(p);
                    break;
                }
            }
        }
        int roll = RNG.nextInt(100);
        int lureChance = AbnormalitiesConfig.TW_MIMICRY_LURE_CHANCE.get();
        List<String> pool;
        if (roll < lureChance) {
            pool = LURE_MESSAGES.getOrDefault(sourceLang, LURE_MESSAGES.get("en_us"));
        } else {
            pool = AMBIENT_MESSAGES.getOrDefault(sourceLang, AMBIENT_MESSAGES.get("en_us"));
        }

            String message = pool.get(RNG.nextInt(pool.size()));
        if (message.contains("X Y Z")) {
            ServerPlayer randomPlayer = players.get(RNG.nextInt(players.size()));
            int cx = randomPlayer.blockPosition().getX() + RNG.nextInt(100) - 50;
            int cy = randomPlayer.blockPosition().getY() + RNG.nextInt(20) - 10;
            int cz = randomPlayer.blockPosition().getZ() + RNG.nextInt(100) - 50;
            message = message.replace("X Y Z", cx + " " + cy + " " + cz);
        }
        String formatted = "<" + sourceName + "> " + message;
        LOGGER.info("[THE_WIND|Mimicry] Chat message: {} (pool={}, lureChance={})", formatted, roll < lureChance ? "lure" : "ambient", lureChance);

        for (ServerPlayer target : players) {
            String targetLang = getPlayerLanguage(target);
            if (targetLang.equals(sourceLang)) {
                sendChat(target, formatted);
            } else {
                List<String> targetPool;
                if (roll < lureChance) {
                    targetPool = LURE_MESSAGES.getOrDefault(targetLang, LURE_MESSAGES.get("en_us"));
                } else {
                    targetPool = AMBIENT_MESSAGES.getOrDefault(targetLang, AMBIENT_MESSAGES.get("en_us"));
                }
                String translated = targetPool.get(RNG.nextInt(targetPool.size()));
                sendChat(target, "<" + sourceName + "> " + translated);
            }
        }
    }

    private static void triggerFakeWhisper(net.minecraft.server.MinecraftServer srv) {
        var players = srv.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        ServerPlayer target = players.get(RNG.nextInt(players.size()));

        boolean isSister = RNG.nextInt(100) < AbnormalitiesConfig.TW_MIMICRY_SISTER_CHANCE.get();
        boolean isLumi = !isSister && RNG.nextInt(100) < AbnormalitiesConfig.TW_MIMICRY_LUMI_CHANCE.get();
        LOGGER.info("[THE_WIND|Mimicry] Whisper: sister={}%, lumi={}%, result={}", AbnormalitiesConfig.TW_MIMICRY_SISTER_CHANCE.get(), AbnormalitiesConfig.TW_MIMICRY_LUMI_CHANCE.get(), isSister ? "Sister" : isLumi ? "Lumi" : "none");

        String sender;
        String advice;
        if (isSister) {
            sender = "Sister";
            String[] wrong = {"He's safe. Don't run.", "It's not real.", "There's nothing there.",
                    "You're imagining things.", "Calm down, nothing is happening.",
                    "The tunnel is clear.", "Don't worry about the sound.",
                    "Nobody is watching you.", "You're alone. Relax.",
                    "It won't come back.", "You're safe now.",
                    "There are no footsteps.", "The door is locked from the inside.",
                    "Nobody followed you here.", "You can sleep now."};
            advice = wrong[RNG.nextInt(wrong.length)];
        } else if (isLumi) {
            sender = "Lumi";
            String[] wrong = {"The tunnel is safe. Go in.", "That chest has nothing bad.",
                    "Follow the music, it leads somewhere good.",
                    "The coordinates are correct.", "There's treasure behind that wall.",
                    "The cave is empty, I checked.", "You can sleep here.",
                    "The stranger is friendly.", "Pick up the item, it's a gift.",
                    "The water is safe to drink.", "Go deeper, there's loot.",
                    "The structure ahead is safe.", "You won't get lost.",
                    "The dark is just night, nothing else."};
            advice = wrong[RNG.nextInt(wrong.length)];
        } else {
            return;
        }

        String lang = getPlayerLanguage(target);
        String[] localized;
        if (isSister) {
            if (lang.equals("tr_tr")) {
                localized = new String[]{"Guvenli kosma.", "O gercek degil.", "Orada hicbişe yok.",
                        "Hayal goruyorsun.", "Sakin ol hicbir sey olmuyor.",
                        "Tunel temiz.", "Sesi dert etme.",
                        "Kimse seni izlemiyor.", "Yalnizsin rahat ol.",
                        "O geri gelmeyecek.", "Simdi guvendesin.",
                        "Adim sesleri yok.", "Kapi icinden kilitli.",
                        "Kimse seni takip etmedi.", "Simdi uyuyabilirsin."};
            } else if (lang.equals("es_es")) {
                localized = new String[]{"Es seguro, no corras.", "No es real.", "No hay nada ahi.",
                        "Te estas imaginando cosas.", "Calmado, no pasa nada.",
                        "El tunel esta limpio.", "No te preocupes por el sonido.",
                        "Nadie te esta observando.", "Estas solo, relajate.",
                        "No volvera.", "Estas a salvo ahora.",
                        "No hay pasos.", "La puerta esta cerrada desde adentro.",
                        "Nadie te siguio hasta aqui.", "Puedes dormir ahora."};
            } else if (lang.equals("fr_fr")) {
                localized = new String[]{"C'est sur, cours pas.", "C'est pas reel.", "Y'a rien la.",
                        "Tu imagines des choses.", "Calme-toi rien se passe.",
                        "Le tunnel est propre.", "T'embete pas pour le bruit.",
                        "Personne t'observe.", "T'es seul, relax.",
                        "Il reviendra pas.", "T'es en securite maintenant.",
                        "Y'a pas de pas.", "La porte est verrouillee de l'interieur.",
                        "Personne t'a suivi ici.", "Tu peux dormir maintenant."};
            } else if (lang.equals("de_de")) {
                localized = new String[]{"Es ist sicher, renn nicht.", "Es ist nicht echt.", "Da ist nichts.",
                        "Du stellst dir Sachen vor.", "Ruhig, hier passiert nichts.",
                        "Der Tunnel ist sauber.", "Mach dir keine Sorgen um den Laut.",
                        "Niemand beobachtet dich.", "Du bist allein, entspann dich.",
                        "Er kommt nicht zurueck.", "Du bist jetzt sicher.",
                        "Es gibt keine Schritte.", "Die Tuer ist von innen abgeschlossen.",
                        "Niemand hat dich hierhin gefolgt.", "Du kannst jetzt schlafen."};
            } else {
                localized = new String[]{"It's safe, don't run.", "It's not real.", "There's nothing there.",
                        "You're imagining things.", "Calm down, nothing is happening.",
                        "The tunnel is clear.", "Don't worry about the sound.",
                        "Nobody is watching you.", "You're alone. Relax.",
                        "It won't come back.", "You're safe now.",
                        "There are no footsteps.", "The door is locked from the inside.",
                        "Nobody followed you here.", "You can sleep now."};
            }
        } else {
            if (lang.equals("tr_tr")) {
                localized = new String[]{"Tunel guvenli, gir.", "O sandikta kotu bir sey yok.",
                        "Sesi takip et, iyi bir yere goturuyor.", "Koordinatlar dogru.",
                        "Duvarin arkasinda hazine var.", "Magara bos, kontrol ettim.",
                        "Burada uyuyabilirsin.", "Yabanci dostane.",
                        "Itemi al, bu bir hediye.", "Su icilebilir.",
                        "Daha derine git, loot var.", "Yapi guvenli.",
                        "Karanliktan kaybolmazsin.", "Oyunun sesi normal.",
                        "Burasi guvenli, ben kontrol ettim."};
            } else if (lang.equals("es_es")) {
                localized = new String[]{"El tunel es seguro, entra.", "Ese cofre no tiene nada malo.",
                        "Sigue el sonido, lleva a algo bueno.", "Las coordenadas son correctas.",
                        "Detras de ese muro hay tesoro.", "La cueva esta vacia, la revise.",
                        "Puedes dormir aqui.", "El extraño es amigable.",
                        "Recoge el objeto, es un regalo.", "El agua es segura para beber.",
                        "Ve mas profundo, hay loot.", "La estructura es segura.",
                        "No te perderas.", "La oscuridad es solo noche.",
                        "Aqui es seguro, lo revise."};
            } else if (lang.equals("fr_fr")) {
                localized = new String[]{"Le tunnel est sur, entre.", "Ce coffre n'a rien de mauvais.",
                        "Suis le son, il mene a quelque chose de bien.", "Les coordonnees sont correctes.",
                        "Derriere ce mur il y a du tresor.", "La grotte est vide, j'ai verifie.",
                        "Tu peux dormir ici.", "L'etranger est amical.",
                        "Ramasse l'objet, c'est un cadeau.", "L'eau est sure a boire.",
                        "Va plus profond, il y a du butin.", "La structure est sure.",
                        "Tu te perdras pas.", "Le noir c'est juste la nuit.",
                        "C'est sur ici, j'ai verifie."};
            } else if (lang.equals("de_de")) {
                localized = new String[]{"Der Tunnel ist sicher, geh rein.", "Der Trunk hat nichts Schlechtes.",
                        "Folge dem Klang, er fuehrt irgendwohin Guten.", "Die Koordinaten sind korrekt.",
                        "Hinter dieser Wand ist ein Schatz.", "Die Hoehle ist leer, ich habe nachgeschaut.",
                        "Du kannst hier schlafen.", "Der Fremde ist freundlich.",
                        "Nimm den Gegenstand, es ist ein Geschenk.", "Das Wasser ist sicher zum Trinken.",
                        "Geh tiefer, da ist Beute.", "Die Struktur ist sicher.",
                        "Du wirst dich nicht verlaufen.", "Die Dunkelheit ist nur Nacht.",
                        "Hier ist sicher, ich habe nachgeschaut."};
            } else {
                localized = new String[]{"The tunnel is safe. Go in.", "That chest has nothing bad.",
                        "Follow the music, it leads somewhere good.", "The coordinates are correct.",
                        "There's treasure behind that wall.", "The cave is empty, I checked.",
                        "You can sleep here.", "The stranger is friendly.",
                        "Pick up the item, it's a gift.", "The water is safe to drink.",
                        "Go deeper, there's loot.", "The structure ahead is safe.",
                        "You won't get lost.", "The dark is just night, nothing else.",
                        "It's safe here, I checked."};
            }
        }

        String chosen = localized[RNG.nextInt(localized.length)];
        sendChat(target, "<" + sender + "> " + chosen);
    }

    private static String getPlayerLanguage(ServerPlayer player) {
        try {
            String lang = player.getLanguage();
            if (lang != null && !lang.isEmpty()) {
                return lang.toLowerCase();
            }
        } catch (Exception ignored) {}
        return "en_us";
    }

    private static List<String> getEssentialFriendNames() {
        List<String> names = new ArrayList<>();
        if (!ModList.get().isLoaded("essential")) return names;
        try {
            Class<?> essentialClass = Class.forName("team.essential.Essential");
            Object api = essentialClass.getMethod("getApi").invoke(null);
            Object friends = api.getClass().getMethod("getFriends").invoke(api);
            if (friends instanceof Collection<?> friendList) {
                for (Object friend : friendList) {
                    String name = (String) friend.getClass().getMethod("getName").invoke(friend);
                    if (name != null && !name.isEmpty()) names.add(name);
                }
            }
            if (!names.isEmpty()) return names;
        } catch (Exception ignored) {}
        try {
            Class<?> apiClass = Class.forName("team.essential.api.EssentialAPI");
            Object api = apiClass.getMethod("getApi").invoke(null);
            Object friends = api.getClass().getMethod("getFriends").invoke(api);
            if (friends instanceof Collection<?> friendList) {
                for (Object friend : friendList) {
                    String name = (String) friend.getClass().getMethod("getName").invoke(friend);
                    if (name != null && !name.isEmpty()) names.add(name);
                }
            }
            if (!names.isEmpty()) return names;
        } catch (Exception ignored) {}
        try {
            Class<?> modClass = Class.forName("team.essential.EssentialMod");
            Object client = modClass.getMethod("getApiClient").invoke(null);
            Object friends = client.getClass().getMethod("getFriends").invoke(client);
            if (friends instanceof Collection<?> friendList) {
                for (Object friend : friendList) {
                    String name = (String) friend.getClass().getMethod("getName").invoke(friend);
                    if (name != null && !name.isEmpty()) names.add(name);
                }
            }
            if (!names.isEmpty()) return names;
        } catch (Exception ignored) {}
        try {
            Class<?> platformClass = Class.forName("team.essential.lib.platform.Platform");
            Object platform = platformClass.getMethod("getServer").invoke(null);
            Object users = platform.getClass().getMethod("getOnlineUsers").invoke(platform);
            if (users instanceof Collection<?> userList) {
                for (Object user : userList) {
                    String name = (String) user.getClass().getMethod("getName").invoke(user);
                    if (name != null && !name.isEmpty()) names.add(name);
                }
            }
            if (!names.isEmpty()) return names;
        } catch (Exception ignored) {}
        try {
            Class<?> platformApiClass = Class.forName("team.essential.api.platform.PlatformAPI");
            Object platform = platformApiClass.getMethod("getServer").invoke(null);
            Object users = platform.getClass().getMethod("getOnlineUsers").invoke(platform);
            if (users instanceof Collection<?> userList) {
                for (Object user : userList) {
                    String name = (String) user.getClass().getMethod("getName").invoke(user);
                    if (name != null && !name.isEmpty()) names.add(name);
                }
            }
            if (!names.isEmpty()) return names;
        } catch (Exception ignored) {}
        LOGGER.debug("[THE_WIND|Mimicry] Essential mod loaded but no friend name API path worked, falling back to fake names");
        return names;
    }

    private static void sendChat(ServerPlayer player, String text) {
        if (player.connection == null) return;
        player.connection.send(new ClientboundSystemChatPacket(Component.literal(text), false));
    }

    public static void forceMimic(ServerPlayer player) {
        String lang = getPlayerLanguage(player);
        String sourceName;
        var srv = player.level().getServer();
        if (srv != null && srv.isSingleplayer()) {
            List<String> friends = getEssentialFriendNames();
            if (!friends.isEmpty()) {
                sourceName = friends.get(RNG.nextInt(friends.size()));
            } else {
                sourceName = com.abnormalities.FakeNames.NAMES.get(RNG.nextInt(com.abnormalities.FakeNames.NAMES.size()));
            }
        } else {
            sourceName = com.abnormalities.FakeNames.NAMES.get(RNG.nextInt(com.abnormalities.FakeNames.NAMES.size()));
        }
        List<String> pool = LURE_MESSAGES.getOrDefault(lang, LURE_MESSAGES.get("en_us"));
        String message = pool.get(RNG.nextInt(pool.size()));
        sendChat(player, "<" + sourceName + "> " + message);
    }

    public static void forceWhisper(ServerPlayer player) {
        triggerFakeWhisper(player.level().getServer());
    }

    public static void forceRandom(ServerPlayer player) {
        if (RNG.nextBoolean()) {
            forceMimic(player);
        } else {
            forceWhisper(player);
        }
    }
}
