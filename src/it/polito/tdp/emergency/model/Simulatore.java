package it.polito.tdp.emergency.model;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import it.polito.tdp.emergency.model.Evento.TipoEvento;
import it.polito.tdp.emergency.model.Paziente.StatoPaziente;

public class Simulatore {

	// Coda degli eventi
	private PriorityQueue<Evento> queue = new PriorityQueue<>() ;
	
	// Modello del Mondo
	private List<Paziente> listaPazienti;
	private PriorityQueue<Paziente> salaAttesa;
	private int studiLiberi;
	
	// Parametri di simulazione
	private int NS = 3; // numero di studi medici
	private int NP = 50; // numero di pazienti in arrivo
	private Duration T_ARRIVAL = Duration.ofMinutes(15); // intervallo di tempo di arrivo tra i pazienti

	private LocalTime T_inizio = LocalTime.of(8, 0);
	private LocalTime T_fine = LocalTime.of(20, 0);

	private int DURATION_TRIAGE = 5;		//assegna colore in 5 minuti
	private int DURATION_WHITE = 10;		//durata visita di quelli con il caretellino bianco
	private int DURATION_YELLOW = 15;
	private int DURATION_RED = 30;
	private int TIMEOUT_WHITE = 120;		//dopo quanto tempo scade il tempo per salvare e curare il paziente
	private int TIMEOUT_YELLOW = 60;
	private int TIMEOUT_RED = 90;

	
	// Statistiche da calcolare
	private int numDimessi;
	private int numAbbandoni;
	private int numMorti;
	
	
	// Variabili interne
	private StatoPaziente nuovoStatoPaziente;	//per capire quale variabile simulare all'interno del simulatore
	private Duration intervalloPoling=Duration.ofMinutes(5);		//ogni quanto bisogna controllare se è stato liberato lo studio medico, se ci sono pazienti in attesa
	
	
	
	
	
	//costruttore
	public Simulatore(){
		this.listaPazienti=new ArrayList<Paziente>();
	}
	
	
	
	
	
	
	//inizializzatore
	public void init() {
		
		//creare pazienti
		listaPazienti.clear();
		LocalTime oraArrivo=T_inizio;
		for(int i=0; i<NP; i++) {
			Paziente p=new Paziente(i+1, oraArrivo);		//dove i+1 suppongo sia l'id del paziente, e il secondo parametro l'ora di arrivo
			listaPazienti.add(p);
			oraArrivo=oraArrivo.plus(T_ARRIVAL);		//il paziente successivo arriva dopo T_ARRIVAL
		}
		this.salaAttesa=new PriorityQueue<>(new PrioritaPaziente());
		
		//creare studi liberi
		studiLiberi=NS;
		nuovoStatoPaziente=StatoPaziente.WAITING_WHITE;
		
		//creare gli eventi iniziali
		queue.clear();
		for(Paziente p:listaPazienti) {
			queue.add(new Evento(p.getOraArrivo(), TipoEvento.ARRIVO, p));
		}
		
		//lancia l'osservatore polling
		queue.add(new Evento(T_inizio.plus(intervalloPoling), TipoEvento.POLLING, null));		//è solo un controllo, non mi interessa un paziente specifico
		
		//resettare statistiche
		numDimessi=0;
		numMorti=0;
		numAbbandoni=0;
		
	}
	
	
	
	
	
	
	
	//eseguzione
	public void run() {
		
		while(!queue.isEmpty()) {		//anche se si arriva all'ora finale si continua a visiare tutti i pazienti arrivati prima di quell'ora che sono ancora in attesa

			
			Evento ev=queue.poll();
			System.out.println(ev.toString());
			Paziente p=ev.getPaziente();

			//se invece mi devo interrompere nel momento esatto in cui arriva l'ora finale: non visito più pazienti dopo
			/*if(ev.getOra().isAfter(T_fine)) {
				break;
			}*/
			
			switch(ev.getTipo()) {
			
			case ARRIVO:
				//dopo 5 minuti dall'arrivo avviene l'evento triage
				queue.add(new Evento(ev.getOra().plusMinutes(DURATION_TRIAGE), TipoEvento.TRIAGE, ev.getPaziente()));
				break;
				
			case TRIAGE:
				
				p.setStato(nuovoStatoPaziente); 	//al primo giro setta lo stato a bianco
				
				//dopo che viene assegnato loro il codice colore si crea un evento timeout
				if(p.getStato()==StatoPaziente.WAITING_WHITE) {
				queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_WHITE), TipoEvento.TIMEOUT, p) );
				
				}else if(p.getStato()==StatoPaziente.WAITING_YELLOW) {
					queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_YELLOW), TipoEvento.TIMEOUT, p) );
					
				}else if(p.getStato()==StatoPaziente.WAITING_RED) {
					queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_RED), TipoEvento.TIMEOUT, p) );	
				}
				
				salaAttesa.add(p);	//si va ad aggiungere un paziente nella sala di attesa in base alla sua priorità
				ruotaNuovoStatoPaziente();		//in rotazione assegna un colore diverso
				
				break;
				
			case VISITA:
				
				//determina il paziente con max priorità 
				Paziente pazienteChiamato=salaAttesa.poll();		//si toglie il cliente dalla lista sala di attesa
				if(pazienteChiamato==null) {
					break;		//se non ci sono più pazienti in lista di attesa, ignoro tutto e passo all'evento successivo
				}
				
				//paziente entra nello studio, 
				StatoPaziente vecchioStatoPaziente=pazienteChiamato.getStato();
				pazienteChiamato.setStato(StatoPaziente.TREATING);		//lo stanno trattando
				
				//lo studio diventa occupato
				studiLiberi--;
				
				//schedula l'uscita
				if(vecchioStatoPaziente==StatoPaziente.WAITING_RED) {
					queue.add(new Evento(ev.getOra().plusMinutes(DURATION_RED), TipoEvento.CURATO, pazienteChiamato));
				}else if(vecchioStatoPaziente==StatoPaziente.WAITING_YELLOW) {
					queue.add(new Evento(ev.getOra().plusMinutes(DURATION_YELLOW), TipoEvento.CURATO, pazienteChiamato));
				}else if(vecchioStatoPaziente==StatoPaziente.WAITING_WHITE) {
					queue.add(new Evento(ev.getOra().plusMinutes(DURATION_WHITE), TipoEvento.CURATO, pazienteChiamato));
				}
				
				break;
				
			case CURATO:
				// paziente fuori
				p.setStato(StatoPaziente.OUT);
				// aggiorna il numDimessi
				numDimessi++;
				
				// schedula evento VISITA "adesso"
				studiLiberi++;
				queue.add(new Evento(ev.getOra(), TipoEvento.VISITA,null));		//si libera uno studio e quindi chiamo il prossimo, senza sapere se c'è qualcuno nella lista di attesa
				
				break;
				
			case TIMEOUT:
				
				//rimuovi dalla lista d'attesa
				salaAttesa.remove(p);
				
				//se è bianco va a casa, se è giallo diventa rosso, se era rosso muore
				if(p.getStato()==StatoPaziente.WAITING_WHITE) {
					p.setStato(StatoPaziente.OUT); // va a casa
					numAbbandoni++;
				}else if(p.getStato()==StatoPaziente.WAITING_YELLOW){
					p.setStato(StatoPaziente.WAITING_RED);
					queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_RED), TipoEvento.TIMEOUT, p));
				}else if (p.getStato()==StatoPaziente.WAITING_RED){
					p.setStato(StatoPaziente.BLACK);
					numMorti++;
				}else {
					System.out.println("Timeout anomalo nello stato"+p.getStato());
				}
				break;
				
			case POLLING:
				
				//verifica se ci sono pazienti in attesa e se ci sono studi liberi, 
				if(!salaAttesa.isEmpty() && studiLiberi>0) {
					queue.add(new Evento(ev.getOra(), TipoEvento.VISITA, null));
				}
				
				//rischedula se stesso
				if(ev.getOra().isBefore(T_fine)) {
				queue.add(new Evento(ev.getOra().plus(intervalloPoling), TipoEvento.POLLING, null));
				}
				
				break;
			
			}
			
			
		}
		
		
		
	}

	
	
	
	
	
	
	/**
	 * Metodo che mi permette di ottenere in rotazione pazienti con colori diversi
	 */
	private void ruotaNuovoStatoPaziente() {

		if(nuovoStatoPaziente==StatoPaziente.WAITING_WHITE) {
			nuovoStatoPaziente=StatoPaziente.WAITING_YELLOW;
		}else if(nuovoStatoPaziente==StatoPaziente.WAITING_YELLOW) {
			nuovoStatoPaziente=StatoPaziente.WAITING_RED;
		}else if(nuovoStatoPaziente==StatoPaziente.WAITING_RED) {
			nuovoStatoPaziente=StatoPaziente.WAITING_WHITE;
		}
		
		
	}

	public int getNS() {
		return this.NS;
	}

	public PriorityQueue<Evento> getQueue() {
		return queue;
	}

	public void setQueue(PriorityQueue<Evento> queue) {
		this.queue = queue;
	}

	public List<Paziente> getListaPazienti() {
		return listaPazienti;
	}

	public void setListaPazienti(List<Paziente> listaPazienti) {
		this.listaPazienti = listaPazienti;
	}

	public int getStudiLiberi() {
		return studiLiberi;
	}

	public void setStudiLiberi(int studiLiberi) {
		this.studiLiberi = studiLiberi;
	}

	public int getNP() {
		return NP;
	}

	public void setNP(int nP) {
		NP = nP;
	}

	public Duration getT_ARRIVAL() {
		return T_ARRIVAL;
	}

	public void setT_ARRIVAL(Duration t_ARRIVAL) {
		T_ARRIVAL = t_ARRIVAL;
	}

	public LocalTime getT_inizio() {
		return T_inizio;
	}

	public void setT_inizio(LocalTime t_inizio) {
		T_inizio = t_inizio;
	}

	public LocalTime getT_fine() {
		return T_fine;
	}

	public void setT_fine(LocalTime t_fine) {
		T_fine = t_fine;
	}

	public int getDURATION_TRIAGE() {
		return DURATION_TRIAGE;
	}

	public void setDURATION_TRIAGE(int dURATION_TRIAGE) {
		DURATION_TRIAGE = dURATION_TRIAGE;
	}

	public int getDURATION_WHITE() {
		return DURATION_WHITE;
	}

	public void setDURATION_WHITE(int dURATION_WHITE) {
		DURATION_WHITE = dURATION_WHITE;
	}

	public int getDURATION_YELLOW() {
		return DURATION_YELLOW;
	}

	public void setDURATION_YELLOW(int dURATION_YELLOW) {
		DURATION_YELLOW = dURATION_YELLOW;
	}

	public int getDURATION_RED() {
		return DURATION_RED;
	}

	public void setDURATION_RED(int dURATION_RED) {
		DURATION_RED = dURATION_RED;
	}

	public int getTIMEOUT_WHITE() {
		return TIMEOUT_WHITE;
	}

	public void setTIMEOUT_WHITE(int tIMEOUT_WHITE) {
		TIMEOUT_WHITE = tIMEOUT_WHITE;
	}

	public int getTIMEOUT_YELLOW() {
		return TIMEOUT_YELLOW;
	}

	public void setTIMEOUT_YELLOW(int tIMEOUT_YELLOW) {
		TIMEOUT_YELLOW = tIMEOUT_YELLOW;
	}

	public int getTIMEOUT_RED() {
		return TIMEOUT_RED;
	}

	public void setTIMEOUT_RED(int tIMEOUT_RED) {
		TIMEOUT_RED = tIMEOUT_RED;
	}

	public int getNumDimessi() {
		return numDimessi;
	}

	public void setNumDimessi(int numDimessi) {
		this.numDimessi = numDimessi;
	}

	public int getNumAbbandoni() {
		return numAbbandoni;
	}

	public void setNumAbbandoni(int numAbbandoni) {
		this.numAbbandoni = numAbbandoni;
	}

	public int getNumMorti() {
		return numMorti;
	}

	public void setNumMorti(int numMorti) {
		this.numMorti = numMorti;
	}

	public StatoPaziente getNuovoStatoPaziente() {
		return nuovoStatoPaziente;
	}

	public void setNuovoStatoPaziente(StatoPaziente nuovoStatoPaziente) {
		this.nuovoStatoPaziente = nuovoStatoPaziente;
	}

	public void setNS(int nS) {
		NS = nS;
	}
	
	
	
	
	
}
