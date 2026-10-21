# StickerPort

App Android e iPhone in italiano per raccogliere gli sticker caricati nelle pagine TikTok e creare pacchetti personali per WhatsApp. Progetto indipendente, non affiliato a TikTok o WhatsApp.

## Utilizzo

1. Incolla un link TikTok e premi **Apri**.
2. Apri i commenti e scorri per caricare gli sticker. Per una tua chat, accedi al sito TikTok nel browser integrato, se quella funzione è disponibile.
3. Premi **Raccogli sticker**, poi tocca le immagini da importare.
4. Crea un pacchetto con 3–30 sticker dello stesso tipo e premi **Aggiungi a WhatsApp**.

Puoi anche importare immagini PNG, JPEG e WebP dal telefono, rinominare pacchetti e rimuovere sticker. Raccolta e immagini vengono conservate nel contenitore privato dell'app. Ogni esportazione crea una copia con identificatore distinto, mantenendo disponibili le copie precedenti per WhatsApp.

## Compatibilità e limiti

- Android 10 o successivo: APK firmato, installazione manuale.
- iOS 16 o successivo: IPA non firmato, da firmare e installare con AltStore Classic/AltServer o strumenti equivalenti. Non è un'app già approvata per AltStore PAL. Durata della firma e rinnovo dipendono dall'account e dal metodo usato.
- Statici: ridimensionamento proporzionale su canvas trasparente 512×512, massimo 100 KB. Se la compressione non basta l'app segnala il limite.
- Animati: WebP originali fino a 512×512, massimo 500 KB, durata massima 10 secondi, fotogrammi di almeno 8 ms. Il canvas viene centrato a 512×512 senza ricodificare i fotogrammi. I file più grandi vengono rifiutati con un messaggio, non convertiti in immagini statiche.
- GIF e video non sono supportati nella versione 0.1.0.
- La raccolta legge le immagini già caricate nel DOM del sito, non interroga API private. TikTok può cambiare il sito, richiedere accesso, limitare il browser integrato o non rendere disponibili i messaggi. Non è garantito il recupero di ogni sticker.
- Un originale animato è stato recuperato da un commento TikTok durante la verifica di fattibilità. Il flusso TikTok e l'importazione effettiva in WhatsApp devono ancora essere verificati su telefoni reali. I test automatici non equivalgono a questa verifica.

## Privacy

Nessun server StickerPort, pubblicità o telemetria. Il browser integrato comunica con TikTok; le immagini selezionate vengono scaricate dai suoi CDN HTTPS. Le sessioni TikTok restano nel contenitore del browser dell'app. Il browser TikTok non contiene il bridge nativo dell'interfaccia locale. L'esportazione invia a WhatsApp solo il pacchetto scelto. Su iPhone usa gli appunti locali con scadenza di 60 secondi, come previsto dal protocollo WhatsApp. Disinstallare l'app elimina la raccolta locale; su Android può rendere non disponibili i file dei pacchetti aggiunti a WhatsApp. Usa contenuti che puoi legittimamente riutilizzare.

## Compilazione e verifiche

`npm test` verifica parser WebP, padding, vincoli pacchetti, raccolta DOM e validazione URL. Non richiede dipendenze npm.

Android: JDK 17, Gradle 8.9, SDK 35, `gradle -p android assembleRelease`. `connectedDebugAndroidTest` verifica WebView/bridge, storage, provider WhatsApp e policy URL su emulatore.

iPhone: macOS con Xcode e XcodeGen, `python3 scripts/create_icon.py`, `xcodegen generate --spec ios/project.yml --project ios`, poi aprire il progetto generato. `scripts/test_ios_simulator.py` verifica l'interfaccia WKWebView, storage e payload WhatsApp. `scripts/package_ipa.py` confeziona esclusivamente una vera build iPhoneOS.

GitHub Actions compila entrambe le piattaforme e conserva gli artefatti. La firma Android avviene separatamente con `scripts/sign_android.ps1`; chiave e password sono escluse da git. Conservare la chiave per gli aggiornamenti futuri.

Protocollo di interoperabilità: [documentazione ufficiale WhatsApp](https://github.com/WhatsApp/stickers). Questa app implementa il contratto senza incorporare sticker di esempio o asset TikTok.
