package com.main.triviatreckapp.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String question;
    private String difficulty;
    private String category;
    
    @Column(name = "correct_answer")
    private String correctAnswer;
    
    @ElementCollection
    @CollectionTable(name = "question_incorrect_answers", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "incorrect_answer")
    private List<String> incorrectAnswers = new ArrayList<>();
    
    @Transient // Non persisté en base, calculé à la volée
    private List<String> options;
    
    @Transient
    private int correctIndex;
    
    /**
     * Calcule la liste complète des options (réponse correcte + incorrectes)
     * @return liste des options
     */
    public List<String> getOptions() {
        if (options == null) {
            options = new ArrayList<>(incorrectAnswers);
            if (correctAnswer != null) {
                // Par défaut on met la bonne réponse à la fin
                // Dans un contexte de quiz on pourrait vouloir mélanger cette liste
                options.add(correctAnswer);
            }
        }
        return options;
    }
    
    /**
     * Détermine l'index de la réponse correcte dans la liste des options
     * @return index de la bonne réponse
     */
    public int getCorrectIndex() {
        return getOptions().indexOf(correctAnswer);
    }
    
    /**
     * Définit l'index de la réponse correcte et réorganise la liste des options
     * @param index Le nouvel index de la réponse correcte
     */
    public void setCorrectIndex(int index) {
        if (options != null && index >= 0 && index < options.size()) {
            // On garde les options mais on réorganise pour que la correcte soit à l'index spécifié
            List<String> allOptions = new ArrayList<>(getOptions());
            correctAnswer = allOptions.get(index);
            allOptions.remove(index);
            incorrectAnswers = allOptions;
        }
    }
}
