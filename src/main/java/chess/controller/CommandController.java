package chess.controller;

import chess.domain.ChessGameService;
import chess.domain.Color;
import chess.domain.ScoreCalculator;
import chess.domain.position.Position;
import chess.dto.CommandDTO;
import chess.dto.PositionParser;
import chess.view.Command;
import chess.view.InputView;
import chess.view.OutputView;
import java.util.EnumMap;
import java.util.Map;

public class CommandController {

    private final InputView inputView;
    private final OutputView outputView;
    private final ChessGameService chessGameService;
    private final Map<Command, CommandExecutor> executors;

    public CommandController(InputView inputView, OutputView outputView) {
        this.inputView = inputView;
        this.outputView = outputView;
        chessGameService = new ChessGameService(new ScoreCalculator());
        executors = initExecutor();
    }

    private Map<Command, CommandExecutor> initExecutor() {
        EnumMap<Command, CommandExecutor> executors = new EnumMap<>(Command.class);
        executors.put(Command.START, this::handleStartCommand);
        executors.put(Command.STATUS, this::handleStatusCommand);
        executors.put(Command.MOVE, this::handleMoveCommand);
        executors.put(Command.END, this::handleEndCommand);
        return executors;
    }

    public State handleCommand(CommandDTO commandDto, Command command) {
        CommandExecutor executor = executors.get(command);
        return executor.execute(commandDto);
    }

    private State handleStartCommand(CommandDTO commandDto) {
        handleInitGame();
        outputView.printBoard(chessGameService.getBoard());
        return State.RUNNING;
    }

    private void handleInitGame() {
        if (chessGameService.isFirstGame() || isRestart()) {
            chessGameService.initNewGame();
        }
    }

    private boolean isRestart() {
        try {
            return inputView.readIsRestart();
        } catch (IllegalArgumentException e) {
            outputView.printError(e);
            return isRestart();
        }
    }

    private State handleMoveCommand(CommandDTO commandDto) {
        Position fromPosition = PositionParser.parse(commandDto.from());
        Position toPosition = PositionParser.parse(commandDto.to());
        chessGameService.handleMove(fromPosition, toPosition);
        outputView.printBoard(chessGameService.getBoard());
        if (chessGameService.isGameOver()) {
            return State.END;
        }
        return State.RUNNING;
    }

    private State handleEndCommand(CommandDTO commandDto) {
        chessGameService.handleEndGame();
        return State.END;
    }

    public void handleEnd() {
        printScore();
        printWinner();
    }

    private State handleStatusCommand(CommandDTO commandDto) {
        printScore();
        return State.RUNNING;
    }

    private void printScore() {
        Map<Color, Double> scores = chessGameService.calculateScore();
        outputView.printScores(scores);
    }

    private void printWinner() {
        Color winner = chessGameService.calculateWinner();
        chessGameService.handleEndGame();
        outputView.printWinner(winner);
    }
}