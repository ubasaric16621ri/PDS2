package org.example.model;

public class Poruka {
    private String autor;
    private String tekst;
    private int id;
    private boolean editovana;
    private String replyNa;

    public Poruka() {}

    public Poruka(String autor, String tekst, int id) {
        this.autor = autor;
        this.tekst = tekst;
        this.id = id;
        this.editovana = false;
        this.replyNa = null;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getTekst() {
        return tekst;
    }

    public void setTekst(String tekst) {
        this.tekst = tekst;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isEditovana() {
        return editovana;
    }

    public void setEditovana(boolean editovana) {
        this.editovana = editovana;
    }

    public String getReplyNa() {
        return replyNa;
    }

    public void setReplyNa(String replyNa) {
        this.replyNa = replyNa;
    }

    @Override
    public String toString() {
        String out = "[" + id + "] " + autor + ": ";
        if (replyNa != null) {
            out += "(odgovor na: \"" + ": " + replyNa + "\") ";
        }
        out += tekst;
        if (editovana) {
            out += " (editovano)";
        }
        return out;
    }
}
