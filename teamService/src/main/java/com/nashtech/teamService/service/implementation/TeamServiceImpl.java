package com.nashtech.teamService.service.implementation;

import com.nashtech.grpc.PlayerRequest;
import com.nashtech.grpc.PlayerResponse;
import com.nashtech.grpc.PlayerServiceGrpc;
import com.nashtech.teamService.entities.Player;
import com.nashtech.teamService.entities.Team;
import com.nashtech.teamService.repository.TeamRepository;
import com.nashtech.teamService.service.TeamService;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link TeamService} interface.
 * <p>
 * This class provides concrete implementations of methods for managing
 * player data using JPA.
 * </p>
 *
 * @author [Nadra Ibrahim]
 */
@Service
public class TeamServiceImpl implements TeamService{

    @GrpcClient("teamService")
    private PlayerServiceGrpc.PlayerServiceBlockingStub serviceBlockingStub;

    private TeamRepository teamRepository;

    public TeamServiceImpl(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * An internal method that retrieves players belonging to a specific team
     * from the Player Service using server streaming.
     * <p>
     * This method sends a request to the Player Service and listens to the stream of PlayerResponse messages.
     * </p>
     *
     * @param teamId The id of the team whose players should be retrieved.
     * @return A list of players retrieved from the stream.
     * @throws StatusRuntimeException If the streaming process is interrupted.
     */
    public List<Player> getPlayersOfTeam(Long teamId) {
        PlayerRequest request = PlayerRequest.newBuilder().setTeamId(teamId).build();
        List<Player> players = new ArrayList<>();
        try {
            Iterator<PlayerResponse> playerResponses = serviceBlockingStub.getPlayersOfTeam(request);
            for (int i = 1; playerResponses.hasNext(); i++) {
                PlayerResponse playerResponse = playerResponses.next();
                Player player = new Player(playerResponse.getPlayerId(), playerResponse.getPlayerName());
                players.add(player);
            }
        } catch (StatusRuntimeException e) {
        }
        return players;
    }

    @Override
    public Team create(Team team) {
        return teamRepository.save(team);
    }

    @Override
    public Team getOne(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(()-> new RuntimeException("Team not found."));
        team.setPlayers(getPlayersOfTeam(team.getTeamId()));
        return team;
    }

    @Override
    public List<Team> getAll() {
        List<Team> teams = teamRepository.findAll();
        List<Team> teamListWithPlayers = teams.stream().map(team -> {
            team.setPlayers(getPlayersOfTeam(team.getTeamId()));
            return team;
        }).collect(Collectors.toList());
        return teamListWithPlayers;
    }
}


