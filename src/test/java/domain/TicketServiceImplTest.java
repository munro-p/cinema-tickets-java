package domain;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidAccountException;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.exception.TooFewAdultsException;
import uk.gov.dwp.uc.pairtest.exception.TooManyTicketsException;

import static org.mockito.Mockito.*;

public class TicketServiceImplTest {

    TicketServiceImpl ticketService;
    SeatReservationService seatReservationService;
    TicketPaymentService ticketPaymentService;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup(){
        seatReservationService = mock(SeatReservationService.class);
        ticketPaymentService = mock(TicketPaymentService.class);
        ticketService = new TicketServiceImpl(seatReservationService, ticketPaymentService);
    }

    @Test
    public void purchaseTicketsWithoutInfantExpectSuccess(){

        // given 3 adults and 2 children
        Long accountId = 1234L;
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);

        // when
        ticketService.purchaseTickets(accountId, adultTickets, childTickets);

        // expect 5 reserved seats, at (£60 + £20)
        verify(seatReservationService, times(1)).reserveSeat(accountId, 5);
        verify(ticketPaymentService, times(1)).makePayment(accountId, 80);
    }

    @Test
    public void purchaseTicketsWithInfantExpectSameSeatsCost(){

        // given 3 adults, 2 children and 3 infants
        Long accountId = 1234L;
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);

        // when
        ticketService.purchaseTickets(accountId, adultTickets, childTickets, infantTickets);

        // expect 5 reserved seats, at (£60 + £20)
        verify(seatReservationService, times(1)).reserveSeat(accountId, 5);
        verify(ticketPaymentService, times(1)).makePayment(accountId, 80);
    }

    @Test
    public void purchaseTicketsExpectSuccess(){

        // given 4 adults, 1 child and 3 infants. mainly to confirm the above wasn't a fluke success
        Long accountId = 1234L;
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 4);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);

        // when
        ticketService.purchaseTickets(accountId, adultTickets, childTickets, infantTickets);

        // expect 5 reserved seats, at (£60 + £20)
        verify(seatReservationService, times(1)).reserveSeat(accountId, 5);
        verify(ticketPaymentService, times(1)).makePayment(accountId, 90);
    }


    @Test
    public void purchaseInvalidRatioExpectException(){

        // given 3 infants for every adult
        Long accountId = 1234L;
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 6);

        // throw an exception
        thrown.expect(InvalidPurchaseException.class);
        ticketService.purchaseTickets(accountId, adultTickets, childTickets, infantTickets);
    }

    @Test
    public void purchaseWithoutAdultExpectException(){

        // given a request with no adults
        Long accountId = 1234L;
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        // throw an exception
        thrown.expect(InvalidPurchaseException.class);
        ticketService.purchaseTickets(accountId, childTickets, infantTickets);
    }

    @Test
    public void purchaseZeroAdultExpectException(){

        // given a request with 0 adults
        Long accountId = 1234L;
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        // throw an exception
        thrown.expect(TooFewAdultsException.class);
        ticketService.purchaseTickets(accountId, adultTickets, childTickets, infantTickets);
    }

    @Test
    public void purchaseWithInvalidAccountExpectException(){

        // given invalid account with ID less than 0
        Long accountId = -2L;
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        // throw an exception
        thrown.expect(InvalidAccountException.class);
        thrown.expectMessage("Invalid account or funds. Cannot purchase tickets using this account.");

        ticketService.purchaseTickets(accountId, adultTickets, childTickets, infantTickets);
    }

    @Test
    public void purchaseTooManyTicketsExpectException(){
        
        // given more than 20 tickets (but not more than 20 seats)
        Long accountId = 1234L;
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 19);
        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);

        // throw an exception
        thrown.expect(TooManyTicketsException.class);
        thrown.expectMessage("Cannot purchase more than 20 tickets in one transaction");

        ticketService.purchaseTickets(accountId, adultTickets, childTickets, infantTickets);
    }

    @Test
    public void purchaseTooFewSeatsExpectException(){

        // given zero seats being purchased
        Long accountId = 1234L;
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);

        // throw an exception. (would be handled by the case of no adults really)
        thrown.expect(TooFewAdultsException.class);
        ticketService.purchaseTickets(accountId, adultTickets);
    }

    @Test
    public void purchaseWithNullAccountIDExpectException(){

        // given no account id
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 5);

        // throw an exception
        thrown.expect(InvalidAccountException.class);
        ticketService.purchaseTickets(null, adultTickets);
    }


    @Test
    public void purchaseWithNullTicketsExpectException(){
        // given no actual tickets being purchased at all
        Long accountId = 123L;

        // throw an exception
        thrown.expect(TooFewAdultsException.class);
        ticketService.purchaseTickets(accountId, null);
    }

}