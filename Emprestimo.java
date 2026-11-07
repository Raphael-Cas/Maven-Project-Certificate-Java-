package com.biblioteca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Represents a row of the "emprestimo" (loan) table. */
@Entity
@Table(name = "emprestimo")
public class Emprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    // With JDBC this class kept the foreign keys as plain numbers (int livroId, int usuarioId).
    // With JPA the foreign key columns are mapped as relationships to the entities themselves.
    // The old numeric ids are still available through getLivroId() / getUsuarioId().
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livro_id")
    private Livro livro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "data_emprestimo")
    private LocalDate dataEmprestimo;

    @Column(name = "data_prevista_devolucao")
    private LocalDate dataPrevistaDevolucao;

    @Column(name = "data_devolucao")
    private LocalDate dataDevolucao; // null while the book has not been returned

    public Emprestimo() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Livro getLivro() {
        return livro;
    }

    public void setLivro(Livro livro) {
        this.livro = livro;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    /** Same value the old "livroId" field held: the id of the borrowed book (0 if none is set). */
    public int getLivroId() {
        return livro != null ? livro.getId() : 0;
    }

    /** Same value the old "usuarioId" field held: the id of the user who borrowed (0 if none is set). */
    public int getUsuarioId() {
        return usuario != null ? usuario.getId() : 0;
    }

    public LocalDate getDataEmprestimo() {
        return dataEmprestimo;
    }

    public void setDataEmprestimo(LocalDate dataEmprestimo) {
        this.dataEmprestimo = dataEmprestimo;
    }

    public LocalDate getDataPrevistaDevolucao() {
        return dataPrevistaDevolucao;
    }

    public void setDataPrevistaDevolucao(LocalDate dataPrevistaDevolucao) {
        this.dataPrevistaDevolucao = dataPrevistaDevolucao;
    }

    public LocalDate getDataDevolucao() {
        return dataDevolucao;
    }

    public void setDataDevolucao(LocalDate dataDevolucao) {
        this.dataDevolucao = dataDevolucao;
    }

    @Override
    public String toString() {
        return "Emprestimo{id=" + id
                + ", livroId=" + getLivroId()
                + ", usuarioId=" + getUsuarioId()
                + ", dataEmprestimo=" + dataEmprestimo
                + ", dataPrevistaDevolucao=" + dataPrevistaDevolucao
                + ", dataDevolucao=" + dataDevolucao + '}';
    }
}
