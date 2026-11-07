package com.biblioteca;

import com.biblioteca.entity.Livro;
import com.biblioteca.persistence.DatabaseException;
import com.biblioteca.persistence.EntityManagerProvider;
import com.biblioteca.repository.LivroRepository;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Entry point of the application. It only shows the menu, reads the user's input
 * and calls the repository. It never talks to the database directly.
 */
public class Main {

    private final Scanner scanner;
    private final LivroRepository repository = new LivroRepository();

    public Main(Scanner scanner) {
        this.scanner = scanner;
    }

    public static void main(String[] args) {
        // The Scanner is closed automatically when the program ends.
        try (Scanner scanner = new Scanner(System.in)) {
            new Main(scanner).run();
        } finally {
            // Closes the EntityManagerFactory (and its connection pool) when the program ends.
            EntityManagerProvider.close();
        }
    }

    // ------------------------------------------------------------------
    // Menu loop
    // ------------------------------------------------------------------

    private void run() {
        boolean running = true;

        while (running) {
            printMenu();
            int option = readInt("Choose an option: ");

            try {
                switch (option) {
                    case 1:
                        registerLivro();
                        break;
                    case 2:
                        listLivros();
                        break;
                    case 3:
                        findLivroById();
                        break;
                    case 4:
                        updateLivro();
                        break;
                    case 5:
                        deleteLivro();
                        break;
                    case 0:
                        running = false;
                        System.out.println("Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid option. Choose a number from the menu.");
                }
            } catch (DatabaseException e) {
                printDatabaseError(e);
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("===== LIBRARY =====");
        System.out.println("1 - Register book");
        System.out.println("2 - List books");
        System.out.println("3 - Find book by ID");
        System.out.println("4 - Update book");
        System.out.println("5 - Delete book");
        System.out.println("0 - Exit");
    }

    // ------------------------------------------------------------------
    // Menu actions
    // ------------------------------------------------------------------

    private void registerLivro() {
        System.out.println("\n--- Register book ---");
        String titulo = readRequired("Title: ");
        String autor = readRequired("Author: ");
        int ano = readPositiveInt("Publication year: ");
        boolean disponivel = readYesNo("Available? (y/n): ");

        Livro livro = new Livro(titulo, autor, ano, disponivel);
        repository.insert(livro);

        System.out.println("Book registered with id " + livro.getId() + ".");
    }

    private void listLivros() {
        System.out.println("\n--- Books ---");
        List<Livro> livros = repository.findAll();

        if (livros.isEmpty()) {
            System.out.println("No books registered.");
            return;
        }
        for (Livro livro : livros) {
            printLivro(livro);
        }
    }

    private void findLivroById() {
        System.out.println("\n--- Find book by ID ---");
        int id = readInt("Book id: ");

        Optional<Livro> livro = repository.findById(id);
        if (livro.isPresent()) {
            printLivro(livro.get());
        } else {
            System.out.println("No book found with id " + id + ".");
        }
    }

    private void updateLivro() {
        System.out.println("\n--- Update book ---");
        int id = readInt("Book id: ");

        Optional<Livro> found = repository.findById(id);
        if (found.isEmpty()) {
            System.out.println("No book found with id " + id + ".");
            return;
        }

        Livro livro = found.get();
        System.out.println("Current data:");
        printLivro(livro);
        System.out.println("Press Enter to keep the current value.");

        String titulo = readLine("Title [" + livro.getTitulo() + "]: ");
        if (!titulo.isEmpty()) {
            livro.setTitulo(titulo);
        }

        String autor = readLine("Author [" + livro.getAutor() + "]: ");
        if (!autor.isEmpty()) {
            livro.setAutor(autor);
        }

        String ano = readLine("Publication year [" + livro.getAnoPublicacao() + "]: ");
        if (!ano.isEmpty()) {
            try {
                int novoAno = Integer.parseInt(ano);
                if (novoAno > 0) {
                    livro.setAnoPublicacao(novoAno);
                } else {
                    System.out.println("Year must be positive. Keeping the current value.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid year. Keeping the current value.");
            }
        }

        String disponivel = readLine("Available (y/n) [" + (livro.isDisponivel() ? "y" : "n") + "]: ");
        if (disponivel.equalsIgnoreCase("y")) {
            livro.setDisponivel(true);
        } else if (disponivel.equalsIgnoreCase("n")) {
            livro.setDisponivel(false);
        } else if (!disponivel.isEmpty()) {
            System.out.println("Invalid answer. Keeping the current value.");
        }

        if (repository.update(livro)) {
            System.out.println("Book updated successfully.");
        } else {
            System.out.println("No book found with id " + id + ". Nothing was updated.");
        }
    }

    private void deleteLivro() {
        System.out.println("\n--- Delete book ---");
        int id = readInt("Book id: ");

        if (repository.delete(id)) {
            System.out.println("Book removed successfully.");
        } else {
            System.out.println("No book found with id " + id + ". Nothing was removed.");
        }
    }

    // ------------------------------------------------------------------
    // Output helpers
    // ------------------------------------------------------------------

    private void printLivro(Livro livro) {
        System.out.println("#" + livro.getId()
                + " | " + livro.getTitulo()
                + " | " + livro.getAutor()
                + " | " + livro.getAnoPublicacao()
                + " | " + (livro.isDisponivel() ? "available" : "on loan"));
    }

    private void printDatabaseError(DatabaseException e) {
        // SQLState 23503 = foreign key violation (the book still has loans).
        if ("23503".equals(e.getSQLState())) {
            System.out.println("Operation refused: this book is referenced by existing loans.");
        } else {
            System.out.println("Database error: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
        }
    }

    // ------------------------------------------------------------------
    // Input helpers (always read whole lines to avoid Scanner newline problems)
    // ------------------------------------------------------------------

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    // Keeps asking until the user types something that is not empty.
    private String readRequired(String prompt) {
        String value = readLine(prompt);
        while (value.isEmpty()) {
            System.out.println("This field is required.");
            value = readLine(prompt);
        }
        return value;
    }

    // Keeps asking until the user types a valid integer.
    private int readInt(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Please type a valid number.");
            }
        }
    }

    // Keeps asking until the user types an integer greater than zero.
    private int readPositiveInt(String prompt) {
        int value = readInt(prompt);
        while (value <= 0) {
            System.out.println("The value must be greater than zero.");
            value = readInt(prompt);
        }
        return value;
    }

    // Keeps asking until the user types y or n.
    private boolean readYesNo(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value.equalsIgnoreCase("y")) {
                return true;
            }
            if (value.equalsIgnoreCase("n")) {
                return false;
            }
            System.out.println("Please type y or n.");
        }
    }
}