package quizbattle;

import java.util.*;

public class QuestionManager {
    private ArrayList<Question> allQuestions = new ArrayList<>();
    private ArrayList<Question> pool = new ArrayList<>();
    private int currentIdx = 0;
    private String difficulty = "easy";

    public QuestionManager() {
        // ── EASY ──────────────────────────────────────────────────
        // Science
        add("What planet is closest to the Sun?", new String[]{"Venus","Mercury","Mars"}, 1, "easy","Science");
        add("What gas do plants need to make food?", new String[]{"Oxygen","Nitrogen","Carbon Dioxide"}, 2, "easy","Science");
        add("How many legs does an insect have?", new String[]{"4","6","8"}, 1, "easy","Science");
        add("What is the boiling point of water in Celsius?", new String[]{"90°C","100°C","110°C"}, 1, "easy","Science");
        add("Which planet is known as the Red Planet?", new String[]{"Jupiter","Mars","Saturn"}, 1, "easy","Science");
        add("What is the center of our solar system?", new String[]{"Moon","Earth","Sun"}, 2, "easy","Science");
        add("How many colors are in a rainbow?", new String[]{"5","6","7"}, 2, "easy","Science");
        add("What do you call a baby dog?", new String[]{"Cub","Puppy","Kitten"}, 1, "easy","Science");
        // History
        add("Who was the first US President?", new String[]{"Lincoln","Jefferson","Washington"}, 2, "easy","History");
        add("Which ancient wonder was located in Egypt?", new String[]{"Colosseum","Great Pyramid","Parthenon"}, 1, "easy","History");
        add("What year did World War II end?", new String[]{"1943","1945","1947"}, 1, "easy","History");
        add("Who invented the telephone?", new String[]{"Edison","Bell","Tesla"}, 1, "easy","History");
        add("Which country gifted the Statue of Liberty to the USA?", new String[]{"UK","France","Germany"}, 1, "easy","History");
        // Geography
        add("What is the capital of France?", new String[]{"Berlin","Paris","Rome"}, 1, "easy","Geography");
        add("What is the largest ocean?", new String[]{"Atlantic","Indian","Pacific"}, 2, "easy","Geography");
        add("Which country has the most people?", new String[]{"USA","China","India"}, 1, "easy","Geography");
        add("What is the capital of Japan?", new String[]{"Beijing","Seoul","Tokyo"}, 2, "easy","Geography");
        add("Which continent is the Sahara Desert on?", new String[]{"Asia","Africa","Australia"}, 1, "easy","Geography");
        // Math
        add("What is 2 + 2?", new String[]{"3","4","5"}, 1, "easy","Math");
        add("What is 10 - 5?", new String[]{"2","5","8"}, 1, "easy","Math");
        add("What is 3 × 3?", new String[]{"6","9","12"}, 1, "easy","Math");
        add("What is 20 ÷ 4?", new String[]{"4","5","6"}, 1, "easy","Math");
        add("What is half of 100?", new String[]{"25","50","75"}, 1, "easy","Math");
        add("How many sides does a triangle have?", new String[]{"2","3","4"}, 1, "easy","Math");
        // Pop Culture
        add("What color is SpongeBob?", new String[]{"Blue","Yellow","Green"}, 1, "easy","Pop Culture");
        add("How many players are on a basketball team (on court)?", new String[]{"4","5","6"}, 1, "easy","Pop Culture");
        add("What animal is Pikachu?", new String[]{"Mouse","Rabbit","Cat"}, 0, "easy","Pop Culture");
        add("In what game do you collect Pokémon?", new String[]{"Digimon","Pokémon GO","Yu-Gi-Oh"}, 1, "easy","Pop Culture");
        add("What is the name of Harry Potter's school?", new String[]{"Narnia","Hogwarts","Wakanda"}, 1, "easy","Pop Culture");
        // Food
        add("What is the main ingredient in guacamole?", new String[]{"Tomato","Avocado","Pepper"}, 1, "easy","Food");
        add("What fruit is known as the 'king of fruits'?", new String[]{"Mango","Durian","Pineapple"}, 1, "easy","Food");
        add("What do you call cooked rice rolled in seaweed?", new String[]{"Sashimi","Sushi","Ramen"}, 1, "easy","Food");

        // ── MEDIUM ────────────────────────────────────────────────
        // Science
        add("What is the chemical symbol for gold?", new String[]{"Ag","Au","Fe"}, 1, "medium","Science");
        add("What is the approximate speed of light (km/s)?", new String[]{"200,000","300,000","400,000"}, 1, "medium","Science");
        add("How many bones are in the adult human body?", new String[]{"186","206","226"}, 1, "medium","Science");
        add("What force keeps planets in orbit?", new String[]{"Magnetism","Gravity","Friction"}, 1, "medium","Science");
        add("What is the powerhouse of the cell?", new String[]{"Nucleus","Ribosome","Mitochondria"}, 2, "medium","Science");
        add("What is the chemical formula for water?", new String[]{"CO2","H2O","NaCl"}, 1, "medium","Science");
        add("How many planets are in our solar system?", new String[]{"7","8","9"}, 1, "medium","Science");
        add("What organ produces insulin?", new String[]{"Liver","Pancreas","Kidney"}, 1, "medium","Science");
        // History
        add("In what year did the Berlin Wall fall?", new String[]{"1987","1989","1991"}, 1, "medium","History");
        add("Who painted the Mona Lisa?", new String[]{"Michelangelo","Da Vinci","Raphael"}, 1, "medium","History");
        add("Which empire built Machu Picchu?", new String[]{"Aztec","Mayan","Inca"}, 2, "medium","History");
        add("Who wrote 'The Art of War'?", new String[]{"Confucius","Sun Tzu","Laozi"}, 1, "medium","History");
        add("What ancient civilization built the Colosseum?", new String[]{"Greek","Roman","Egyptian"}, 1, "medium","History");
        add("Who was the first person to walk on the moon?", new String[]{"Buzz Aldrin","Neil Armstrong","Yuri Gagarin"}, 1, "medium","History");
        // Geography
        add("What is the longest river in the world?", new String[]{"Amazon","Mississippi","Nile"}, 2, "medium","Geography");
        add("Which country has the most natural lakes?", new String[]{"Russia","Canada","USA"}, 1, "medium","Geography");
        add("What is the capital of Australia?", new String[]{"Sydney","Melbourne","Canberra"}, 2, "medium","Geography");
        add("What is the smallest country in the world?", new String[]{"Monaco","San Marino","Vatican City"}, 2, "medium","Geography");
        add("Which mountain is the tallest in the world?", new String[]{"K2","Everest","Kilimanjaro"}, 1, "medium","Geography");
        // Math
        add("What is the square root of 144?", new String[]{"11","12","13"}, 1, "medium","Math");
        add("What is 15% of 200?", new String[]{"25","30","35"}, 1, "medium","Math");
        add("If x + 7 = 15, what is x?", new String[]{"6","7","8"}, 2, "medium","Math");
        add("What is 2 to the power of 8?", new String[]{"128","256","512"}, 1, "medium","Math");
        add("How many degrees are in a right angle?", new String[]{"45°","90°","180°"}, 1, "medium","Math");
        // Technology
        add("What does CPU stand for?", new String[]{"Central Process Unit","Central Processing Unit","Computer Processing Unit"}, 1, "medium","Technology");
        add("What language is used for web styling?", new String[]{"HTML","CSS","PHP"}, 1, "medium","Technology");
        add("What does URL stand for?", new String[]{"Universal Resource Link","Uniform Resource Locator","Unique Record Locator"}, 1, "medium","Technology");
        add("Which company created the Android OS?", new String[]{"Apple","Google","Microsoft"}, 1, "medium","Technology");
        add("What does RAM stand for?", new String[]{"Random Access Memory","Read Access Module","Rapid Action Memory"}, 0, "medium","Technology");
        // Pop Culture
        add("Which movie features 'May the Force be with you'?", new String[]{"Star Trek","Star Wars","Interstellar"}, 1, "medium","Pop Culture");
        add("Who sang 'Thriller'?", new String[]{"Prince","Michael Jackson","Madonna"}, 1, "medium","Pop Culture");
        add("What is the highest-grossing movie of all time?", new String[]{"Titanic","Avengers: Endgame","Avatar"}, 2, "medium","Pop Culture");
        add("In which sport do you score a 'hat trick'?", new String[]{"Basketball","Soccer/Football","Tennis"}, 1, "medium","Pop Culture");

        // ── HARD ──────────────────────────────────────────────────
        // Science
        add("What is the atomic number of carbon?", new String[]{"4","6","8"}, 1, "hard","Science");
        add("What is the half-life of Carbon-14 (approximate years)?", new String[]{"3,700","5,730","8,000"}, 1, "hard","Science");
        add("What is Newton's 2nd Law formula?", new String[]{"E=mc²","F=ma","P=mv"}, 1, "hard","Science");
        add("Which part of the brain controls balance?", new String[]{"Cerebrum","Hippocampus","Cerebellum"}, 2, "hard","Science");
        add("What is the most abundant gas in Earth's atmosphere?", new String[]{"Oxygen","Nitrogen","Argon"}, 1, "hard","Science");
        add("What particle has no electric charge?", new String[]{"Proton","Electron","Neutron"}, 2, "hard","Science");
        add("Which law states PV = nRT?", new String[]{"Boyle's Law","Ideal Gas Law","Charles' Law"}, 1, "hard","Science");
        // History
        add("In what year was the Magna Carta signed?", new String[]{"1215","1315","1415"}, 0, "hard","History");
        add("Who was the last Tsar of Russia?", new String[]{"Nicholas I","Alexander III","Nicholas II"}, 2, "hard","History");
        add("The Battle of Thermopylae was between which two sides?", new String[]{"Rome vs Carthage","Greece vs Persia","Egypt vs Assyria"}, 1, "hard","History");
        add("What year did the French Revolution begin?", new String[]{"1776","1789","1804"}, 1, "hard","History");
        add("Which treaty ended World War I?", new String[]{"Treaty of Paris","Treaty of Versailles","Treaty of Ghent"}, 1, "hard","History");
        // Geography
        add("What is the capital of Kazakhstan?", new String[]{"Almaty","Astana","Shymkent"}, 1, "hard","Geography");
        add("Which country has the most time zones?", new String[]{"Russia","USA","France"}, 2, "hard","Geography");
        add("What is the deepest lake in the world?", new String[]{"Caspian Sea","Lake Superior","Lake Baikal"}, 2, "hard","Geography");
        add("What is the capital of Mozambique?", new String[]{"Maputo","Harare","Lusaka"}, 0, "hard","Geography");
        add("The Strait of Malacca separates which two landmasses?", new String[]{"Japan and Korea","Malay Peninsula and Sumatra","India and Sri Lanka"}, 1, "hard","Geography");
        // Math
        add("What is the value of Pi to 4 decimal places?", new String[]{"3.1415","3.1416","3.1417"}, 1, "hard","Math");
        add("What is the derivative of sin(x)?", new String[]{"cos(x)","-cos(x)","tan(x)"}, 0, "hard","Math");
        add("How many prime numbers are below 20?", new String[]{"7","8","9"}, 1, "hard","Math");
        add("What is the sum of angles in a hexagon?", new String[]{"540°","720°","900°"}, 1, "hard","Math");
        add("What is log₂(64)?", new String[]{"4","5","6"}, 2, "hard","Math");
        add("What is the Fibonacci number after 55?", new String[]{"77","89","98"}, 1, "hard","Math");
        // Technology
        add("What does SQL stand for?", new String[]{"Structured Query Language","Simple Query Logic","System Query Language"}, 0, "hard","Technology");
        add("What is the time complexity of binary search?", new String[]{"O(n)","O(log n)","O(n²)"}, 1, "hard","Technology");
        add("What year was the first iPhone released?", new String[]{"2006","2007","2008"}, 1, "hard","Technology");
        add("What does HTTPS stand for?", new String[]{"Hyper Transfer Text Protocol Secure","HyperText Transfer Protocol Secure","High Transfer Text Protocol Security"}, 1, "hard","Technology");
        add("Which sorting algorithm has O(n log n) average complexity?", new String[]{"Bubble Sort","Quick Sort","Insertion Sort"}, 1, "hard","Technology");
        // Pop Culture & Literature
        add("Who directed the movie 'Inception'?", new String[]{"Spielberg","Nolan","Villeneuve"}, 1, "hard","Pop Culture");
        add("In chess, how many squares are on the board?", new String[]{"48","56","64"}, 2, "hard","Pop Culture");
        add("What is the most spoken language in the world by native speakers?", new String[]{"English","Spanish","Mandarin"}, 2, "hard","Pop Culture");
        add("Who wrote '1984'?", new String[]{"Huxley","Orwell","Kafka"}, 1, "hard","Literature");
        add("In Shakespeare's Hamlet, who is Hamlet's father's ghost?", new String[]{"King Claudius","King Hamlet","King Lear"}, 1, "hard","Literature");
        add("What year did the first Star Wars film release?", new String[]{"1975","1977","1979"}, 1, "hard","Pop Culture");
    }

    private void add(String text, String[] opts, int correct, String diff, String topic) {
        allQuestions.add(new Question(text, opts, correct, diff, topic));
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
        rebuildPool();
    }

    private void rebuildPool() {
        pool.clear();
        for (Question q : allQuestions) {
            if (q.difficulty.equals(difficulty)) pool.add(q);
        }
        Collections.shuffle(pool);
        currentIdx = 0;
    }

    public void reset() {
        rebuildPool();
    }

    public Question next() {
        if (pool.isEmpty()) rebuildPool();
        if (currentIdx >= pool.size()) {
            Collections.shuffle(pool);
            currentIdx = 0;
        }
        return pool.get(currentIdx++);
    }
}
