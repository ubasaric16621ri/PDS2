package org.example.klijent;
import org.example.model.Poruka;
import org.eclipse.paho.client.mqttv3.*;

import java.util.*;

public class Klijent {
    private static final String broker = "tcp://localhost:1883";
    private static final String temaSobe = "sobe";
    private static final Scanner scanner = new Scanner(System.in);
    private static MqttClient client;
    private static String sobaId;
    private static String korisnickoIme;
    private static int brojacPoruka = 1;
    private static Map<Integer, Poruka> primljenePoruke = new HashMap<>();

    public static void main(String[] args) {
        try {
            System.out.print("Unesi svoje ime: ");
            korisnickoIme = scanner.nextLine();

            client = new MqttClient(broker, MqttClient.generateClientId());
            MqttConnectOptions opcije = new MqttConnectOptions();
            opcije.setCleanSession(true);
            client.connect(opcije);

            List<String> dostupneSobe = new ArrayList<>();

            client.subscribe(temaSobe, (topic, msg) -> {
                String novaSoba = new String(msg.getPayload());
                if (!dostupneSobe.contains(novaSoba)) {
                    dostupneSobe.add(novaSoba);
                    System.out.println("Pronađena soba: " + novaSoba);
                }
            });

            System.out.println("Čekam dostupne sobe (5 sekundi)...");
            Thread.sleep(5000);

            if (dostupneSobe.isEmpty()) {
                System.out.println("Nema dostupnih soba.");
                return;
            }

            System.out.print("Unesi ID sobe kojoj želiš da pristupiš: ");
            sobaId = scanner.nextLine();

            // Zahtev za pristup
            String temaZahtev = "zahtev/" + sobaId;
            client.publish(temaZahtev, new MqttMessage(korisnickoIme.getBytes()));

            // Čekanje odgovora
            String temaDozvole = "dozvola/" + sobaId + "/" + korisnickoIme;
            final boolean[] odobreno = {false};

            client.subscribe(temaDozvole, (topic, msg) -> {
                String odgovor = new String(msg.getPayload());
                if (odgovor.equals("dozvoljeno")) {
                    odobreno[0] = true;
                } else {
                    System.out.println("Pristup odbijen.");
                    System.exit(0);
                }
            });

            // Sačekaj odgovor (max 5 sek)
            System.out.println("Čekam potvrdu za ulazak...");
            for (int i = 0; i < 10; i++) {
                if (odobreno[0]) break;
                Thread.sleep(500);
            }

            if (!odobreno[0]) {
                System.out.println("Nije stigao odgovor. Izlazim.");
                return;
            }

            System.out.println("Pristup sobi dozvoljen!");

            // Chat soba
            String temaChat = "chat/" + sobaId;
            client.subscribe(temaChat, (topic, msg) -> {
                Poruka poruka = deserialize(new String(msg.getPayload()));
                primljenePoruke.put(poruka.getId(), poruka);
                if (!poruka.getAutor().equals(korisnickoIme)) {
                    System.out.println(poruka);
                }
            });

            while (true) {
                System.out.print("> ");
                String unos = scanner.nextLine();

                if (unos.startsWith("edit:")) {
                    String[] delovi = unos.split(":", 3);
                    if (delovi.length == 3) {
                        int id = Integer.parseInt(delovi[1]);
                        if (primljenePoruke.containsKey(id)) {
                            Poruka stara = primljenePoruke.get(id);
                            if (stara.getAutor().equals(korisnickoIme)) {
                                Poruka nova = new Poruka(korisnickoIme, delovi[2], id);
                                nova.setEditovana(true);
                                client.publish(temaChat, new MqttMessage(serialize(nova).getBytes()));
                            } else {
                                System.out.println("Ne možeš da menjaš tuđu poruku.");
                            }
                        } else {
                            System.out.println("Poruka sa tim ID ne postoji.");
                        }
                        continue;
                    }
                }

                Poruka poruka;
                if (unos.contains("@")) {
                    String[] delovi = unos.split(":", 2);
                    String deo = delovi[0].substring(1).trim();
                    String tekst = delovi.length > 1 ? delovi[1].trim() : "";
                    poruka = new Poruka(korisnickoIme, tekst, brojacPoruka++);
                    poruka.setReplyNa(deo);
                } else {
                    poruka = new Poruka(korisnickoIme, unos, brojacPoruka++);
                }

                client.publish(temaChat, new MqttMessage(serialize(poruka).getBytes()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String serialize(Poruka p) {
        return p.getId() + "|" + p.getAutor() + "|" + p.getTekst() + "|" +
                (p.getReplyNa() == null ? "null" : p.getReplyNa()) + "|" + (p.isEditovana() ? "1" : "0");
    }

    private static Poruka deserialize(String s) {
        String[] delovi = s.split("\\|");
        int id = Integer.parseInt(delovi[0]);
        String autor = delovi[1];
        String tekst = delovi[2];
        String reply = delovi[3].equals("null") ? null : delovi[3];
        boolean editovana = delovi[4].equals("1");

        Poruka p = new Poruka(autor, tekst, id);
        p.setReplyNa(reply);
        p.setEditovana(editovana);
        return p;
    }
}
