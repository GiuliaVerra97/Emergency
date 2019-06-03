package it.polito.tdp.emergency.model;

import java.time.LocalTime;

/**
 * Evento per la simulazione di Emergency
 *
 */
public class Evento implements Comparable<Evento>{

	/**
	 * Tipologie di eventi che possono accadere 
	 */
	public enum TipoEvento {
		ARRIVO, // un nuovo paziente arriva in Emergency
		TRIAGE, // infermiere assegna codice colore dopo 5 minuti da quando arriva il paziente
		VISITA, // paziente viene visitato da medico, se si libera uno studio medico
		CURATO, // paziente esce da studio medico
		TIMEOUT, // attesa troppo lunga per essere visitato
		POLLING,  //evento che verifica periodicamente se ci sono studi liberi e pazienti in attesa
	}
	
	private LocalTime ora ; // timestamp dell'evento
	private TipoEvento tipo ; // tipologia
	private Paziente paziente ; // chi è il paziente coinvolto nell'evento
	
	public Evento(LocalTime ora, TipoEvento tipo, Paziente paziente) {
		super();
		this.ora = ora;
		this.tipo = tipo;
		this.paziente = paziente;
	}
	public LocalTime getOra() {
		return ora;
	}
	public TipoEvento getTipo() {
		return tipo;
	}
	public Paziente getPaziente() {
		return paziente;
	}
	
	// Ordina per orario dell'evento
	@Override
	public int compareTo(Evento other) {
		return this.ora.compareTo(other.ora);
	}
	@Override
	public String toString() {
		return String.format("Evento [ora=%s, tipo=%s, paziente=%s]", ora, tipo, paziente);
	}
	
	
	
	
	
	
	
}
