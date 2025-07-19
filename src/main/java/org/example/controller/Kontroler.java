package org.example.controller;

import org.eclipse.paho.client.mqttv3.*;
import java.util.*;

public class Kontroler {
    private static final String broker = "tcp://localhost:1883";
    private static final String temaSobe = "sobe";
    private static final Scanner scanner = new Scanner(System.in);
    private static MqttClient client;
    private static String sobaId;

    public static void main(String[] args) {
        try {
            sobaId = UUID.randomUUID().toString().substring(0, 5);
            client = new MqttClient(broker, MqttClient.generateClientId());
            MqttConnectOptions opcije = new MqttConnectOptions();
            opcije.setCleanSession(true);
            client.connect(opcije);

            System.out.println("Soba kreirana sa ID: " + sobaId);

            // Višestruka objava sobe da bi klijent video
            for (int i = 0; i < 5; i++) {
                client.publish(temaSobe, new MqttMessage(sobaId.getBytes()));
                Thread.sleep(500);
            }

            // Pretplata na zahteve
            client.subscribe("zahtev/" + sobaId, (topic, msg) -> {
                String korisnik = new String(msg.getPayload());
                System.out.println("Korisnik '" + korisnik + "' traži pristup sobi.");

                System.out.print("Dozvoli korisniku? (da/ne): ");
                String odgovor = scanner.nextLine();

                String temaDozvole = "dozvola/" + sobaId + "/" + korisnik;
                if (odgovor.equalsIgnoreCase("da")) {
                    client.publish(temaDozvole, new MqttMessage("dozvoljeno".getBytes()));
                    System.out.println("Dozvoljeno.");
                } else {
                    client.publish(temaDozvole, new MqttMessage("odbijeno".getBytes()));
                    System.out.println("Odbijeno.");
                }
            });

            System.out.println("Čekam korisnike...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
