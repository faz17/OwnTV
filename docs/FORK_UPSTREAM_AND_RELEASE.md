# faz17 OwnTV Fire TV — sviluppo e aggiornamenti

Questo fork resta separato dal codice originale del progetto `ahXN00/OwnTV`.
Il ramo `feature/dispatcharr-source-switch` contiene le personalizzazioni
Dispatcharr, la gestione DVR, la navigazione e le traduzioni per Fire TV.

## Aggiornamenti ufficiali

Il workflow `.github/workflows/upstream-sync-dispatcharr.yml` vive sul
ramo predefinito `main` e controlla quotidianamente il ramo `main`
del repository ufficiale. Se esistono nuovi commit:

1. Esegue il merge sul ramo personalizzato **localmente**, senza usare `--force`.
2. Verifica l'inventario i18n e compila/tests OwnTV contro il ramo
   personalizzato di `faz17/OwnTV_Core`.
3. Se tutto passa, aggiorna `feature/dispatcharr-source-switch` e poi
   il ramo `main`. Non elimina mai le modifiche specifiche Dispatcharr.
4. In caso di conflitto o di test fallito, non pubblica il merge e apre
   un issue nel fork. Occorre risolverlo prima di rilasciare una nuova APK.

Il fork `faz17/OwnTV_Core` controlla upstream prima di TV e Mobile;
`faz17/OwnTV_Mobile` utilizza un workflow equivalente.

## APK Fire TV e rilascio iniziale

Il workflow `dispatcharr-apk.yml` produce una APK Android TV installabile
mediante Downloader e aggiorna la prerelease
`dispatcharr-tv-preview` solo dopo la compilazione.

La prima release versionata `v6.0.0` usa una APK **release firmata**,
non una APK debug, e viene pubblicata una sola volta quando i controlli
passano e sono disponibili i segreti di firma nel repository.
L'asset pubblico è `OwnTV-FireTV-v6.0.0.apk`.

Le successive release versionate devono usare un tag e una versione
maggiori, ad esempio `v6.0.1`, senza sostituire la APK di `v6.0.0`.
Il controllo aggiornamenti interno del fork legge le release
`faz17/OwnTV`, non quelle del repository originale.

### Firma e identità

L'application ID rimane `tv.own.owntv`, per conservare dati e preferenze
nell'aggiornamento in-place. Una APK futura deve essere firmata con **lo
stesso certificato** già usato dal fork. Una chiave di firma diversa
richiede reinstallazione e può comportare la perdita dei dati locali.
Non inserire file di firma o credenziali nel repository.

### Icona

L'app ufficiale supporta più varianti di icone tramite il proprio
selettore App Icon. Un'icona di fork completamente diversa può essere
aggiunta come nuova variante, comprendendo icona launcher e banner TV.
La grafica va approvata prima di sostituire risorse o cambiare
l'application ID; una diversa icona da sola non richiede un nuovo ID.
