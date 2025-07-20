package org.example.klijent;

import org.example.model.Poruka;
import org.eclipse.paho.client.mqttv3.*;

import java.util.*;

public class Klijent extends Thread {
    private static final String broker = "tcp://localhost:1883";

    private final String korisnickoIme;
    private final String sobaId;
    private MqttClient client;
    private final Map<Integer, Poruka> primljenePoruke = new HashMap<>();
    private int brojacPoruka = 1;

    public Klijent(String korisnickoIme, String sobaId) {
        this.korisnickoIme = korisnickoIme;
        this.sobaId = sobaId;
    }

    @Override
    public void run() {
        try {
            client = new MqttClient(broker, MqttClient.generateClientId());
            MqttConnectOptions opcije = new MqttConnectOptions();
            opcije.setCleanSession(true);
            client.connect(opcije);

            String temaChat = "chat/" + sobaId;
            client.subscribe(temaChat);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Veza sa brokerom prekinuta.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    Poruka poruka = deserialize(new String(message.getPayload()));
                    primljenePoruke.put(poruka.getId(), poruka);
                    if (!poruka.getAutor().equals(korisnickoIme)) {
                        System.out.println(poruka);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            String temaZahtev = "zahtev/" + sobaId;
            client.publish(temaZahtev, new MqttMessage(korisnickoIme.getBytes()));

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

            System.out.println("[" + korisnickoIme + "] Čekam dozvolu...");
            for (int i = 0; i < 10; i++) {
                if (odobreno[0]) break;
                Thread.sleep(500);
            }

            if (!odobreno[0]) {
                System.out.println("Nije stigla dozvola.");
                return;
            }

            System.out.println("[" + korisnickoIme + "] Pristup dozvoljen!");

            Scanner sc = new Scanner(System.in);
            while (true) {
                System.out.print("[" + korisnickoIme + "] > ");
                String unos = sc.nextLine();

                if (unos.startsWith("edit:")) {
                    String[] delovi = unos.split(":", 3);
                    if (delovi.length == 3) {
                        int id = Integer.parseInt(delovi[1]);
                        if (primljenePoruke.containsKey(id)) {
                            Poruka stara = primljenePoruke.get(id);
                            if (stara.getAutor().equals(korisnickoIme)) {
                                Poruka nova = new Poruka(korisnickoIme, delovi[2], id);
                                nova.setEditovana(true);
                                MqttMessage m = new MqttMessage();
                                m.setPayload(serialize(nova).getBytes());
                                client.publish(temaChat, m);
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
                    String tekst = "";
                    if (delovi.length > 1) {
                        tekst = delovi[1].trim();
                    }
                    poruka = new Poruka(korisnickoIme, tekst, brojacPoruka++);
                    poruka.setReplyNa(deo);
                } else {
                    poruka = new Poruka(korisnickoIme, unos, brojacPoruka++);
                }

                MqttMessage m = new MqttMessage();
                m.setPayload(serialize(poruka).getBytes());
                client.publish(temaChat, m);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String serialize(Poruka p) {
        String s = "";
        s = s + p.getId() + "|";
        s = s + p.getAutor() + "|";
        s = s + p.getTekst() + "|";
        if (p.getReplyNa() == null) {
            s = s + "null" + "|";
        } else {
            s = s + p.getReplyNa() + "|";
        }
        if (p.isEditovana()) {
            s = s + "1";
        } else {
            s = s + "0";
        }
        return s;
    }

    private Poruka deserialize(String s) {
        String[] delovi = s.split("\\|");
        int id = Integer.parseInt(delovi[0]);
        String autor = delovi[1];
        String tekst = delovi[2];
        String reply;

        if (delovi[3].equals("null")) {
            reply = null;
        } else {
            reply = delovi[3];
        }

        boolean editovana;
        if (delovi[4].equals("1")) {
            editovana = true;
        } else {
            editovana = false;
        }

        Poruka p = new Poruka(autor, tekst, id);
        p.setReplyNa(reply);
        p.setEditovana(editovana);
        return p;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Unesi ime: ");
        String ime = sc.nextLine();
        System.out.print("Unesi ID sobe: ");
        String soba = sc.nextLine();
        Klijent k = new Klijent(ime, soba);
        k.start();
    }
}
