package org.example.sociable_number;

import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CancellationException;

public class HelloController {
    @FXML private TextField upperLimit;
    @FXML private TextField threadCount;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Label errorLabel;
    @FXML private Label statusLabel;
    @FXML private FlowPane workerPane;
    @FXML private TextArea resultsArea;
    @FXML private Label sociableTotal;
    @FXML private Label otherTotal;
    @FXML private Label checkedTotal;

    private Run activeRun;

    private static final class Run {
        final int limit;
        final List<Task<List<Integer>>> workers = new ArrayList<>();
        int finished;

        Run(int limit) {
            this.limit = limit;
        }
    }

    @FXML
    private void initialize() {
        upperLimit.setText("10000");
        threadCount.setText("4");
        setError("");
        upperLimit.textProperty().addListener((observable, previous, value) -> setError(""));
        threadCount.textProperty().addListener((observable, previous, value) -> setError(""));
        displayTotals(0, 0);
        statusLabel.setText("Ready.");
        setRunning(false);
    }

    @FXML
    private void startRun() {
        if (activeRun != null) {
            return;
        }
        int limit = positiveInteger(upperLimit.getText());
        int count = positiveInteger(threadCount.getText());
        if (limit == 0) {
            setError("Upper limit must be an integer between 1 and 2147483647.");
            return;
        }
        int maxWorkers = Math.min(limit, 64);
        if (count == 0 || count > maxWorkers) {
            setError("Thread count must be an integer between 1 and " + maxWorkers + ".");
            return;
        }

        setError("");
        clearResults();
        workerPane.getChildren().clear();
        Run run = new Run(limit);
        activeRun = run;
        setRunning(true);
        for (SociableNumbers.Range range : SociableNumbers.ranges(limit, count)) {
            Task<List<Integer>> worker = createWorker(range, limit);
            run.workers.add(worker);
            addWorkerCard(run, worker, range, run.workers.size());
        }
        updateRunningStatus(run);
        for (int index = 0; index < run.workers.size(); index++) {
            Thread thread = new Thread(run.workers.get(index), "sociable-worker-" + (index + 1));
            thread.setDaemon(true);
            thread.start();
        }
    }

    private static int positiveInteger(String text) {
        try {
            return Math.max(0, Integer.parseInt(text.strip()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static Task<List<Integer>> createWorker(SociableNumbers.Range range, int limit) {
        return new Task<>() {
            @Override
            protected List<Integer> call() {
                List<Integer> matches = new ArrayList<>();
                for (long number = range.first(); number <= range.last(); number++) {
                    if (isCancelled()) {
                        throw new CancellationException();
                    }
                    if (SociableNumbers.isSociable((int) number, limit)) {
                        matches.add((int) number);
                    }
                    updateProgress(number - range.first() + 1, range.size());
                }
                return matches;
            }
        };
    }

    private void addWorkerCard(Run run, Task<List<Integer>> worker,
                               SociableNumbers.Range range, int number) {
        ProgressIndicator progress = new ProgressIndicator(0);
        progress.setPrefSize(42, 42);
        progress.progressProperty().bind(Bindings.max(0, worker.progressProperty()));
        Label interval = new Label(range.first() + " to " + range.last());
        interval.setMaxWidth(Double.MAX_VALUE);
        interval.setWrapText(true);
        Label state = new Label("Waiting");
        String assignment = range.size() + (range.size() == 1 ? " number" : " numbers");
        VBox card = new VBox(6, new Label("Thread " + number), progress,
                interval, new Label(assignment), state);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(150);
        workerPane.getChildren().add(card);

        worker.setOnRunning(event -> state.setText("Running"));
        worker.setOnSucceeded(event -> {
            if (activeRun == run) {
                progress.progressProperty().unbind();
                progress.setProgress(1);
                state.setText("Done");
                workerFinished(run);
            }
        });
        worker.setOnFailed(event -> {
            if (activeRun == run) {
                stopRun();
                statusLabel.setText("Run failed. Thread " + number + " could not finish.");
            }
        });
    }

    private void workerFinished(Run run) {
        run.finished++;
        if (run.finished < run.workers.size()) {
            updateRunningStatus(run);
            return;
        }
        StringJoiner sortedMatches = new StringJoiner(", ");
        int sociable = 0;
        // Ranges and their individual results are both in ascending order.
        for (Task<List<Integer>> worker : run.workers) {
            List<Integer> matches = worker.getValue();
            sociable += matches.size();
            for (int match : matches) {
                sortedMatches.add(Integer.toString(match));
            }
        }
        resultsArea.setText(sortedMatches.toString());
        displayTotals(sociable, run.limit);
        statusLabel.setText("Complete. Checked all " + run.limit + " numbers from 1 to "
                + run.limit + ". Finished threads: " + run.finished + " of " + run.workers.size() + ".");
        activeRun = null;
        setRunning(false);
    }

    private void updateRunningStatus(Run run) {
        statusLabel.setText("Running. Finished threads: " + run.finished + " of " + run.workers.size() + ".");
    }

    @FXML
    private void stopRun() {
        if (activeRun == null) {
            return;
        }
        shutdown();
        workerPane.getChildren().clear();
        clearResults();
        setRunning(false);
        statusLabel.setText("Stopped.");
    }

    void shutdown() {
        Run run = activeRun;
        if (run != null) {
            activeRun = null;
            for (Task<List<Integer>> worker : run.workers) {
                worker.cancel(true);
            }
        }
    }

    private void clearResults() {
        resultsArea.clear();
        displayTotals(0, 0);
    }

    private void displayTotals(int sociable, int checked) {
        sociableTotal.setText("Sociable numbers: " + sociable);
        otherTotal.setText("Other numbers: " + (checked - sociable));
        checkedTotal.setText("Total checked: " + checked);
    }

    private void setError(String error) {
        errorLabel.setText(error);
        errorLabel.setManaged(!error.isEmpty());
        errorLabel.setVisible(!error.isEmpty());
    }

    private void setRunning(boolean running) {
        upperLimit.setDisable(running);
        threadCount.setDisable(running);
        startButton.setDisable(running);
        stopButton.setDisable(!running);
    }
}
