package it.polito.tdp.emergency.model;

import java.time.LocalTime;

public class Paziente {

	
	//a ogni paziente viene assegnato un colore a seconda dell'urgenza di quel paziente, oppure si pu trattare
	//di un paziente nuovo o in trattamento o che viene dimesso
	public enum StatoPaziente{
		NEW, 
		WAITING_WHITE,
		WAITING_YELLOW,
		WAITING_RED,
		TREATING,		//in trattamento
		OUT,			//dimesso
		BLACK,
	}
	
	
	
	private int id;
	private StatoPaziente stato;
	private LocalTime oraArrivo;		//quando due pazienti hanno la stessa priority passa prima quello che è arrivato prima
	
	
	
	public Paziente(int id, LocalTime oraArrivo) {
		this.id=id;
		this.oraArrivo=oraArrivo;
		this.stato=StatoPaziente.NEW;	//arriva il paziente
	}



	public int getId() {
		return id;
	}



	public void setId(int id) {
		this.id = id;
	}



	public StatoPaziente getStato() {
		return stato;
	}



	public void setStato(StatoPaziente stato) {
		this.stato = stato;
	}



	public LocalTime getOraArrivo() {
		return oraArrivo;
	}



	public void setOraArrivo(LocalTime oraArrivo) {
		this.oraArrivo = oraArrivo;
	}



	@Override
	public String toString() {
		return String.format("[id=%s]", id);
	}
	
	
	
	
	
	
}
