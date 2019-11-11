/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//
//              CAMBIARE GLI ATTRIBUTI DELLA CLASSE IN BASE AL DISPOSITIVO!
//
//
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
package orion_to_thingsboard;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
 
public class MainClass {
 
    //TODO: IDEA INIZIALE(DA CAPIRE PERCHE' NON FUNZIONA):
    //private static HashMap<String,Device> dispositivi;
    //MAPPIAMO <MONGODB_ID, OGGETTO DI TIPO DISPOSITIVO>. QUINDI MI ARRIVEREBBE DA ORION IL
    //DATO CON IL MONGODB_ID, E GRAZIE ALLA MAPPA VADO A PESCARE LE INFO DEL DEVICE SU THINGSBOARD
    //IN PRATICA, PER QUALCHE MOTIVO NON FUNZIONA E SI OTTENGONO DEI NULL. SI E'DECISO DI USARE UN
    //SET INVECE DI UN HASHMAP, SOLUZIONE CHE DOVREBBE ESSERE PIU' LENTA
 
    private static Set<Device> dispositivi;
    //SI CREA QUESTO JSON E LO SI PUSHA IN MQTT A THINGSBOARD COME UPDATE DEI DATI
    private static String fileDiAppoggioJson = "C:\\Users\\Simone\\Desktop\\pino.json";
    //FILE DOVE STA LA MAPPATURA TIPO-UUID1-UUID4-TOKEN
    private static String fileDiInfoThingsboard = "C:\\Users\\Simone\\Desktop\\save.txt";
    //PATH DEL COMANDO mosquitto_pub
    private static String pathMQTTcommand = "C:\\mosquitto\\mosquitto_pub.exe";
    private static String pathDataFromFile = "C:\\Users\\Simone\\Desktop\\datiReali.txt";
    private static int port = 12346;
     
    public static void main(String args[]) {
         
        dispositivi = new HashSet<>();
        readThingsboardComponents(fileDiInfoThingsboard); //CARICO DA FILE LA MAPPATURA "TIPO-UUID1-UUID4-TOKEN" IN APPOSITI OGGETTI DI CLASSE "DEVICE"
        boolean orionAvailable = true;//FALSE: LEGGO I DATI DA FILE. 
                                       //TRUE: LI PRENDO DA ORION
        if(orionAvailable) {//LOOP IN ASCOLTO DI  ORION
            receiveFromOrion();
        }
        else {//NON SI USA ORION MA UN FILE
            readFileTeo(pathDataFromFile);
        }
    }
 
    private static void receiveFromOrion() {
        try {
              @SuppressWarnings("resource")
            ServerSocket ss = new ServerSocket(port);
     
              for (;;) {//LOOP DI ASCOLTO DELLA PORTA. LEGGO IL DATO RICEVUTO DA ORION E FACCIO UN UPDATE DEL DISPOSITIVO SU THINGSBOARD
                Socket client = ss.accept();
                System.out.println("Connessione riuscita");
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
     
                String line;
                while ((line = in.readLine()) != null) {//LEGGO PER RIGHE NEL SOCKET
                  if (line.length() == 0) {//DOPO LA RIGA LUNGA 0 SI TROVA LA RIGA CON I DATI
                          if((line = in.readLine()) != null) {//LEGGO LA RIGA COI DATI
                          System.out.println("PAYLOAD:");
                          System.out.println(line);
                          updateData(line);//UPDATE THINGSBOARD
                          break;
                      }
                  }
                }
                System.out.println();
                 
                in.close();
                client.close();
              }
            }
            catch (Exception e) {
              System.err.println(e);
              System.err.println("Usage: java HttpMirror <port>");
            }
    }
     
    public static void readFileTeo(String path) {
//      int numLine = 0;
        try(BufferedReader buffer=new BufferedReader(new InputStreamReader(new FileInputStream(path)))){
            String line;
            while ((line = buffer.readLine()) != null) {
 
                  if (!line.startsWith("13") && !line.equals("")) {//SCARTA LE RIGHE DI INTESTAZIONE, PRENDE LE RIGHE COI DATI
//                    numLine++;
//                    System.out.println("PAYLOAD:");
//                    System.out.println(line); 
                      updateData(line);             //UPDATE SU THINGSBOARD
//                    System.out.println(numLine);
                  }
                }
        }catch(Exception e){
            System.err.println(e);
        }
        System.out.println("FINE LETTURA TESTO");
    }
 
    private static void readThingsboardComponents(String path) {//LEGGO IL FILE CON INFO SUGLI ID DI THINGSBOARD E CREO GLI OGGETTI CORRISPONDENTI
        try(BufferedReader buffer=new BufferedReader(new InputStreamReader(new FileInputStream(path)))){
            String line;
            String vett[];
            while((line = buffer.readLine())!=null) {
                System.out.println(line);
                vett = line.split(",");
                if(vett.length == 4) {
                    dispositivi.add(new Device(vett[0], vett[1], vett[3], vett[2]));//type, thingsboardId, thingsboardAccessToken, id
                }
            }
        }catch(Exception e){
            System.err.println(e);
        }
        System.out.println("FINE LETTURA DEI DISPOSITIVI");
    }
 
    public static void updateData(String info) {
//IMPORTANTE!!!!: PER INVIARE IL MESSAGGIO MQTT E' RICHIESTO UN FILE DI FORMATO JSON. ANCHE I DATI CHE ARRIVANO DA ORION SONO IN JSON.
//SI POTREBBE QUINDI PENSARE DI CREARE IL FILE JSON COPIANDO IL BODY RICEVUTO DA ORION, E INVIARLO A THINGSBOARD TRAMITE MQTT.
//TUTTAVIA, PARE CHE THINGSBOARD NON ACCETTI NEI JSON IL FORMATO "CHIAVE":{.......}, MA SOLO "CHIAVE":"VALORE". DOBBIAMO QUINDI ESTRARRE I CAMPI
//E COSTRUIRE UN JSON ADATTO DA PUSHARE A THINGSBOARD.
                 
//QUESTA FUNZIONE HA COME PARAMETRO IL BODY OTTENUTO DA ORION. IL BODY E' ANALIZZATO ED E' CREATA UNA MAPPA KEY-VALUE.
//IN QUESTA MAPPA SI TIENE ANCORA IL FORMATO "CHIAVE":{.....}, MA PIU AVANTI ESTRARREMO DALLE PARENTESI GRAFFE IL DATO CHE CI INTERESSA.
          String data = info.substring(info.indexOf('['), info.lastIndexOf(']'));
          data = data.substring(2, data.length()-1);
          System.out.println(data);
           
          String key;
          String value;
          TreeMap<String,String> mappa = new TreeMap<>();
           
//LA VARIABILE DATA HA I DATI RICEVUTI DA ORION/FILE. RIEMPIAMO LA MAPPA CHIAVE-VALORE
//DATA HA IL FORMATO ->  "KEY":"VALUE","KEY2":"VALUE2", ECC....,    DOBBIAMO ESTRARRE KEY E VALUE E INSERIRLI NELLA MAPPA
 
          while(data.length()!=0) {
              data = data.substring(1);
              key = data.substring(0,data.indexOf("\""));
              data = data.substring(key.length()+2);
              //System.out.println(key);
               
              if(data.charAt(0)=='{') {//DOBBIAMO GESTIRE IL CASO ->  "KEY":{"key1":"value2", ... }
                  value = gestisciParentesi(data);
                  mappa.put(key, value);
                   
                  data = data.substring(1);
                  if((value.length()+2)>data.length())
                      data="";
                  else
                      data = data.substring(value.length()+2);
                  continue;
              }
              data = data.substring(1);
              value = data.substring(0,data.indexOf("\""));
              data = data.substring(value.length()+2);
              //System.out.println(value);
               
              mappa.put(key, value);
          }
          //System.out.println(data);
          
//CREATA LA MAPPA GUARDIAMO IL TIPO, E CHIAMIAMO L'APPOSITA FUNZIONE DI AGGIORNAMENTO
//QUESTE FUNZIONI CREERANNO IL CONTENUTO DEL JSON DA PUSHARE A THINGSBOARD IN MQTT
        if(mappa.get("type").equals("Company")) {
            updateCompany(mappa);
            return;
        }
        if(mappa.get("type").equals("Farm")) {
            updateFarm(mappa);
            return;
        }
        if(mappa.get("type").equals("Building")) {
            updateBuilding(mappa);
            return;
        }
        if(mappa.get("type").equals("Compartment")) {
            updateCompartment(mappa);
            return;
        }
        if(mappa.get("type").equals("Pen")) {
            updatePen(mappa);
            return;
        }
        if(mappa.get("type").equals("Pig")) {
            updatePig(mappa);
            return;
        }
         
    }
 
    private static void updatePig(TreeMap<String,String> mappa) {
        String pigId = mappa.get("pigId").split(",")[1].split(":")[1].replace("\"", "");
        String updateData = "{";
         
        updateData = appendData(mappa, "weight", updateData);
        updateData = appendData(mappa, "serialNumber", updateData);
        updateData = appendData(mappa, "lastUpdate", updateData);
        updateData = appendData(mappa, "endTimestampAcquisition", updateData);
        updateData = appendData(mappa, "endTimestampMonitoring", updateData);
        updateData = appendData(mappa, "startTimestampAcquisition", updateData);
        updateData = appendData(mappa, "startTimestampMonitoring", updateData);
        updateData = appendData(mappa, "totalConsumedFood", updateData);
        updateData = appendData(mappa, "totalConsumedWater", updateData);
        updateData = appendData(mappa, "totalTimeConsumedFood", updateData);
        updateData = appendData(mappa, "totalTimeConsumedWater", updateData);
        //TODO: MANCA ADDITIONAL DATA
         
        updateData = updateData.substring(0, updateData.length()-1);//TAGLIA LA VIRGOLA FINALE
        updateData += "}";
        System.out.println(updateData);
             
        for(Device d : dispositivi) {
            if(d.getId().contains(pigId)) {
                update(d.getThingsboardAccessToken(), updateData);
                return;
            }
        }
    }
 
    private static void updatePen(TreeMap<String,String> mappa) {
        String penId = mappa.get("penId").split(",")[1].split(":")[1].replace("\"", "");
        String updateData = "{";
         
        updateData = appendData(mappa, "temperature", updateData);
        updateData = appendData(mappa, "humidity", updateData);
        updateData = appendData(mappa, "luminosity", updateData);
        updateData = appendData(mappa, "waterConsumption", updateData);
        updateData = appendData(mappa, "feedConsumption", updateData);
        updateData = appendData(mappa, "CO2", updateData);
        updateData = appendData(mappa, "numPigs", updateData);
        updateData = appendData(mappa, "numAnimals", updateData);
        updateData = appendData(mappa, "avgWeight", updateData);
        updateData = appendData(mappa, "avgGrowth", updateData);
        updateData = appendData(mappa, "weightStDev", updateData);
        updateData = appendData(mappa, "lastUpdate", updateData);
        //TODO: MANCA ADDITIONAL DATA
         
        updateData = updateData.substring(0, updateData.length()-1);//TAGLIA LA VIRGOLA FINALE
        updateData += "}";
        System.out.println(updateData);
             
        for(Device d : dispositivi) {
            if(d.getId().contains(penId)) {
                update(d.getThingsboardAccessToken(), updateData);
                return;
            }
        }
    }
 
    private static void updateCompartment(TreeMap<String,String> mappa) {
 
        String compartmentId = mappa.get("compartmentId").split(",")[1].split(":")[1].replace("\"", "");
        String updateData = "{";
         
        updateData = appendData(mappa, "temperature", updateData);
        updateData = appendData(mappa, "humidity", updateData);
        updateData = appendData(mappa, "luminosity", updateData);
        updateData = appendData(mappa, "CO2", updateData);
        updateData = appendData(mappa, "numAnimals", updateData);
        updateData = appendData(mappa, "avgWeight", updateData);
        updateData = appendData(mappa, "avgGrowth", updateData);
        updateData = appendData(mappa, "weightStDev", updateData);
        updateData = appendData(mappa, "lastUpdate", updateData);
        //TODO: MANCA ADDITIONAL DATA
         
        updateData = updateData.substring(0, updateData.length()-1);//TAGLIA LA VIRGOLA FINALE
        updateData += "}";
        System.out.println(updateData);
             
        for(Device d : dispositivi) {
            if(d.getId().contains(compartmentId)) {
                update(d.getThingsboardAccessToken(), updateData);
                return;
            }
        }
         
    }
 
    private static void updateBuilding(TreeMap<String,String> mappa) {
        String buildingId = mappa.get("buildingId").split(",")[1].split(":")[1].replace("\"", "");
        String updateData = "{";
         
        updateData = appendData(mappa, "temperature", updateData);
        updateData = appendData(mappa, "humidity", updateData);
        updateData = appendData(mappa, "luminosity", updateData);
        updateData = appendData(mappa, "CO2", updateData);
        updateData = appendData(mappa, "lastUpdate", updateData);
        //TODO: MANCA ADDITIONAL DATA
         
        updateData = updateData.substring(0, updateData.length()-1);//TAGLIA LA VIRGOLA FINALE
        updateData += "}";
        System.out.println(updateData);
             
        for(Device d : dispositivi) {
            if(d.getId().contains(buildingId)) {
                update(d.getThingsboardAccessToken(), updateData);
                return;
            }
        }
    }
 
    private static void updateFarm(TreeMap<String,String> mappa) {//SE HO CAPITO, FARM E COMPANY NON SI DEVONO AGGIORNARE. SONO STATE MESSE COMUNQUE LE FUZIONI VUOTE PER ESPANSIONI FUTURE
    }
 
    private static void updateCompany(TreeMap<String,String> mappa) {
    }
     
    private static String appendData(TreeMap<String, String> mappa, String field, String updateData) {
        String value = mappa.get(field);
        if(value==null)
            return updateData;
        value = value.split(",")[1].split(":")[1].replace("\"", "");
        if(value!=null && !value.equals("")) {
            updateData += "\""+ field +"\":\"" + value + "\",";
        }
        return updateData;
    }
     
    private static void update(String thingsboardAccessToken, String updateData) {//CREA IL JSON E INVIA IL MESSAGGIO MQTT DI AGGIORNAMENTO A THINGSBOARD
        try(FileWriter fw = new FileWriter(fileDiAppoggioJson)){
            fw.write(updateData);
        }catch(Exception e) {
            e.printStackTrace();
        }
        String command[] = {pathMQTTcommand,"-d","-h","127.0.0.1","-t","v1/devices/me/telemetry","-u",thingsboardAccessToken,"-f",fileDiAppoggioJson};
        ProcessBuilder process = new ProcessBuilder(command); 
        Process p;
        try
        {
            p = process.start();
             BufferedReader reader =  new BufferedReader(new InputStreamReader(p.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String line = null;
                while ( (line = reader.readLine()) != null) {
                        builder.append(line);
                        builder.append(System.getProperty("line.separator"));
                }
                String result = builder.toString();
                System.out.print(result);
         
        }
        catch (IOException e)
        {   System.out.print("error");
            e.printStackTrace();
        }
    }
     
      private static String gestisciParentesi(String data) {
//LA FUNZIONE E' CHIAMATA NEL CASO ABBIA "CHIAVE":{"SOTTO_CHIAVE1":"VALORE1","SOTTO_CHIAVE2":"VALORE2", ...}
//E SERVE AD ESTRARRE LA PARTE TRA IL PRIMO LIVELLO DI PARENTESI { ..... }
//SERVE AVERE QUESTA FUNZIONE PERCHE' POTREI AVERE IL CASO -> "CHIAVE":{"SOTTO_CHIAVE1":{....}, ...} E VOGLIO ESTRARRE SOLO
//IL CONTENUTO DEL PRIMO LIVELLO DI PARENTESI SENZA TOCCARE GLI ALTRI LIVELLI
          data = data.substring(1);
          int aperte = 1;
          int chiuse = 0;
          int i=0;
           
          while(aperte != chiuse) {
              if(data.charAt(i)=='{') {
                  aperte++;
              }
              if(data.charAt(i)=='}') {
                  chiuse++;
              }
              i++;
          }
           
          String value = "";
          value = data.substring(0,i-1);
          //System.out.println(value);
          if((value.length()+2)>data.length())
              data="";
          else
              data = data.substring(value.length()+2);
 
          return value;
      }
}