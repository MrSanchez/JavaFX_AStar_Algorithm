package main;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Cell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.*;

public class A_STAR extends Application {

    private final static int cellSize = 30;

    private final static int gridRows = 20;
    private final static int gridColumns = 40;

    private int nodeCount = 0;

    private Cell[][] cells;

    private Cell start;
    private Cell goal;

    private Stage stage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.cells = new Cell[gridRows][gridColumns];
        Pane root = this.createGrid();

        Scene scene = new Scene(root);
        stage = primaryStage;
        stage.setTitle("A* Algorithm Search");
        stage.setResizable(false);
        stage.sizeToScene();
        stage.setScene(scene);
        stage.show();
    }

    private void updateWindowTitle()
    {
        String title = String.format("A* Algorithm Search | nodes checked: %d", nodeCount);
        stage.setTitle(title);
    }

    private GridPane createGrid()
    {
        GridPane grid = new GridPane();

        for(int row = 0; row < gridRows; row++)
        {
            for(int col = 0; col < gridColumns; col++)
            {
                Cell cell = new Cell(cellSize, cellSize, row, col);
                cell.setFill(Color.WHITE);
                cell.setStroke(Color.BLACK);

                cell.setOnMousePressed(e -> {
                    if(cell.isObstacle()) return;

                    if(e.isShiftDown())
                    {
                        start = cell;
                        cell.setFill(Color.GREEN);
                        checkForGoalAndStart();
                    } else if(e.isAltDown()) {
                        goal = cell;
                        cell.setFill(Color.RED);
                        checkForGoalAndStart();
                    }
                });
                cells[row][col] = cell;
                grid.add(cell, col, row);
            }
        }

        return grid;
    }

    private void checkForGoalAndStart()
    {
        if(start != null && goal != null)
        {
            this.clearAStarVisited();
            ArrayList<Cell> path = A_Star(start, goal);

            for(Cell cell: path)
            {
                cell.highlightAsPath();
            }
            this.updateWindowTitle();
        }
    }

    private void clearAStarVisited() {
        this.nodeCount = 0;
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridColumns; col++) {
                Cell cell = cells[row][col];
                if(cell.isObstacle() || cell == goal || cell == start)
                {
                    // dont do nuffin
                } else {
                    cell.clear();
                }
            }
        }
    }

    public ArrayList<Cell> getCellNeighbours(Cell cell)
    {
        final int range = 1;

        int cellRow = cell.getRow();
        int cellCol = cell.getCol();

        int startRow = cellRow - range;
        int startCol = cellCol - range;

        int endRow = Math.min(cellRow + range, gridRows - 1);
        int endCols = Math.min(cellCol + range, gridColumns - 1);

        ArrayList<Cell> neighbours = new ArrayList<>();
        for(int row = startRow; row <= endRow; row++)
        {
            if(row < 0) continue;
            for(int col = startCol; col <= endCols; col++)
            {
                if(col < 0) continue;
                if(row == cellRow && col == cellCol) {
                    // Our cell
                } else {
                    Cell neighbour = cells[row][col];
                    if(!neighbour.isObstacle())
                        neighbours.add(neighbour);
                }
            }
        }
        return neighbours;
    }

    public ArrayList<Cell> A_Star(Cell startCell, Cell goalCell)
    {
        // Cells that have been checked and are not part of the best path(?)
        Set<Cell> closedSet = new HashSet<Cell>();

        // Cells that have been found but not been checked yet
        // Starting with the first starting cell
        Set<Cell> openSet = new HashSet<Cell>();
        openSet.add(startCell);

        Map<Cell, Cell> bestPrevious = new HashMap<>();

        // Total cost from startCell to Cell.
        Map<Cell, Integer> gScore = new HashMap<>();

        // From start to start = 0
        gScore.put(startCell, 0);

        // Cost from startCell to Cell + heuristic from Cell to goalCell (f(cell) = g(cell) + h(cell))
        Map<Cell, Double> fScore = new HashMap<>();

        // gScore for startCell is 0, so fScore is 0 + heuristic
        fScore.put(startCell, heuristicCost(startCell, goalCell));

        while(!openSet.isEmpty())
        {
            Cell currentCell = getLowestFscore(openSet, fScore);
            if(currentCell.equals(goalCell)) return reconstructPath(bestPrevious, currentCell);

            openSet.remove(currentCell);
            closedSet.add(currentCell);
            currentCell.highlightAsVisited();

            ArrayList<Cell> neighbours = getCellNeighbours(currentCell);
            for (Cell neighbour : neighbours)
            {
                neighbour.highlightAsNeighbour();
                if(closedSet.contains(neighbour))
                    continue; // Neighbour has already been checked. Don't bother checking again.

                int tempGScore = gScore.get(currentCell) + costBetween(currentCell, neighbour);

                if(!openSet.contains(neighbour)) { // Neighbour hasn't been discovered before
                    openSet.add(neighbour);
                } else if(tempGScore >= gScore.get(neighbour)) { // Neighbour has been discovered before
                    continue; // And the current gScore hasn't improved.
                }
                // In all other cases, it's the best path

                bestPrevious.put(neighbour, currentCell);
                gScore.put(neighbour, tempGScore);
                double heuristicCost = heuristicCost(neighbour, goalCell);
                System.out.printf("(%d, %d) g(n): %d, h(n): %.2f, f(n): %.2f\n", neighbour.getRow(), neighbour.getCol(), tempGScore, heuristicCost, tempGScore + heuristicCost);
                fScore.put(neighbour, tempGScore + heuristicCost);
            }
        }
        return new ArrayList<>();
    }

    private int costBetween(Cell currentCell, Cell neighbour) {
        int currentRow = currentCell.getRow();
        int currentCol = currentCell.getCol();

        int neighbourRow = neighbour.getRow();
        int neighbourCol = neighbour.getCol();

        if(currentRow == neighbourRow || currentCol == neighbourCol)
            return 10;
        else
            return 14;
    }

/*
    // Euclidean
    private double heuristicCost(Cell currentCell, Cell goalCell) {
        int x = currentCell.getRow() - goalCell.getRow();
        int y = currentCell.getCol() - goalCell.getCol();
        double cost = Math.sqrt((x*x)+(y*y)); // Get distance between two points
        this.nodeCount++;
        return cost * 10.0;
    }*/

    // Diagonal distance
    private double heuristicCost(Cell currentCell, Cell goalCell) {
        int x = Math.abs(currentCell.getRow() - goalCell.getRow());
        int y = Math.abs(currentCell.getCol() - goalCell.getCol());
        double cost = 10.0 * (x + y) + (14.0 - 2 * 10.0) * Math.min(x, y);
        this.nodeCount++;
        return cost; // + 1.0 / 100.0
    }

    private ArrayList<Cell> reconstructPath(Map<Cell,Cell> bestPrevious, Cell currentCell) {
        ArrayList<Cell> bestPath = new ArrayList<>();
        while (bestPrevious.containsKey(currentCell))
        {
            currentCell = bestPrevious.get(currentCell);
            bestPath.add(currentCell);
        }
        return bestPath;
    }

    private Cell getLowestFscore(Set<Cell> openSet, Map<Cell, Double> fScore) {
        double lowestScore = Double.MAX_VALUE;
        Cell lowestScoringCell = openSet.iterator().next();

        Iterator<Cell> i = openSet.iterator();
        while (i.hasNext()) {
            Cell cell = i.next();
            double score = fScore.get(cell);
            if(score < lowestScore)
            {
                lowestScore = score;
                lowestScoringCell = cell;
            }
        }

        return lowestScoringCell;
    }


    public static void main(String[] args) {
        launch(args);
    }


    class Cell extends Rectangle {

        private boolean obstacle = false;
        private int row, col;

        public Cell(double width, double height, int row, int col) {
            super(width, height);
            this.row = row;
            this.col = col;
            this.addEventhandlers();
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }

        public void highlightAsPath()
        {
            if(!this.equals(start) && !this.equals(goal))
                this.setFill(Color.BLUE);
        }

        public void highlightAsNeighbour()
        {
            if(!this.equals(start) && !this.equals(goal) && this.getFill() != Color.YELLOW)
                this.setFill(Color.ORANGE);
        }

        public void highlightAsVisited()
        {
            if(!this.equals(start) && !this.equals(goal))
                this.setFill(Color.YELLOW);
        }

        public void clear()
        {
            this.setFill(Color.WHITE);
        }

        public void setObstacle(boolean isObstacle)
        {
            if(isObstacle)
            {
                this.setFill(Color.BLACK);
            } else {
                this.setFill(Color.WHITE);
            }
            this.obstacle = isObstacle;
        }

        public boolean isObstacle() {
            return obstacle;
        }

        private void addEventhandlers() {
            this.setOnMouseDragged(e -> {

                if(e.isAltDown() || e.isShiftDown()) return;

                Node node = e.getPickResult().getIntersectedNode();

                if(node instanceof Cell) {
                    Cell selectedCell = (Cell) node;

                    if(e.isPrimaryButtonDown()) {
                        if (!selectedCell.isObstacle() && selectedCell != goal && selectedCell != start) {
                            selectedCell.setObstacle(true);
                            checkForGoalAndStart();
                        }

                    } else if(e.isSecondaryButtonDown()) {
                        if (selectedCell.isObstacle() && selectedCell != goal && selectedCell != start) {
                            selectedCell.setObstacle(false);
                            checkForGoalAndStart();
                        }
                    }
                }
            });
        }
    }
}
