package be.blit.mailcleaner;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/blocked-senders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BlockedSendersResource {

    @Inject
    BlockedSendersService blockedSendersService;

    @GET
    public Response list() {
        return Response.ok(blockedSendersService.getBlockedSenders()).build();
    }

    @POST
    public Response add(BlockedSender blockedSender) {
        blockedSendersService.addBlockedSender(blockedSender.getEmail());
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/{email}")
    public Response remove(@PathParam("email") String email) {
        blockedSendersService.removeBlockedSender(email);
        return Response.noContent().build();
    }

}