package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidAccountException;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.exception.TooFewAdultsException;
import uk.gov.dwp.uc.pairtest.exception.TooManyTicketsException;

import java.util.Arrays;
import java.util.HashMap;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */
    private SeatReservationService seatReservationService;
    private TicketPaymentService ticketPaymentService;

    public TicketServiceImpl(SeatReservationService seatReservationService, TicketPaymentService ticketPaymentService){
        this.seatReservationService = seatReservationService;
        this.ticketPaymentService = ticketPaymentService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        // don't bother doing anything if the request is altogether invalid
        if (accountId == null || accountId <= 0) throw new InvalidAccountException();
        else if (ticketTypeRequests == null || ticketTypeRequests.length == 0) throw new TooFewAdultsException();

        // transform to friendlier structure and carry out all other validations
        HashMap<TicketTypeRequest.Type, Integer> tickets = generatePurchaseData(ticketTypeRequests);
        validatePurchase(tickets);

        // work out total costs and seats required
        int totalCost = (getSeats(tickets, Type.ADULT) * Type.ADULT.cost) + (getSeats(tickets, Type.CHILD) * Type.CHILD.cost);
        int totalSeats = getSeats(tickets, Type.ADULT) + getSeats(tickets, Type.CHILD);

        ticketPaymentService.makePayment(accountId, totalCost);
        seatReservationService.reserveSeat(accountId, totalSeats);
    }

    private HashMap<Type, Integer> generatePurchaseData(TicketTypeRequest... ticketTypeRequests){

        HashMap<Type, Integer> ticketsToPurchase = new HashMap<>();

        // no assumption made that only one of each type of ticket request will come through (though would be an
        // odd UI) so transform collection to hashmap with each ticket type (k) and number of seats requested (v)
        Arrays.stream(ticketTypeRequests).forEach(newRequest -> {
            Type ticketType = newRequest.getTicketType();
            int amount = newRequest.getNoOfTickets();

            if (ticketsToPurchase.containsKey(ticketType))
                ticketsToPurchase.replace(ticketType, ticketsToPurchase.get(ticketType) + amount );
            else
                ticketsToPurchase.put(ticketType, amount);
        });

        return ticketsToPurchase;
    }

    private void validatePurchase(HashMap<TicketTypeRequest.Type, Integer> tickets) {

        if(getSeats(tickets,(Type.ADULT)) == 0 || (getSeats(tickets, Type.ADULT) < getSeats(tickets, Type.INFANT))){
            throw new TooFewAdultsException();
        }

        else if (getSeats(tickets, Type.ADULT) + getSeats(tickets, Type.CHILD) + getSeats(tickets, Type.INFANT) > 20) {
            throw new TooManyTicketsException();
        }
    }

    private Integer getSeats(HashMap<Type, Integer> tickets, Type key){

        // purely for convenience/less busy code
        return tickets.getOrDefault(key, 0);
    }


}
