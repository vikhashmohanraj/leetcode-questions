class ThroneInheritance {
    private Person king;
    private final Map<String, Person> persons;

    public ThroneInheritance(final String kingName) {
        this.king = new Person(kingName);

        this.persons = new HashMap<>();

        this.persons.put(kingName, this.king);
    }
    
    public void birth(final String parentName, final String childName) {
        final Person child = new Person(childName);

        this.persons.get(parentName).children().add(child);
        this.persons.put(childName, child);
    }
    
    public void death(final String name) {
        this.persons.get(name).isAlive(false);
    }
    
    public List<String> getInheritanceOrder() {
        List<String> result = new ArrayList<>();

        this.dfs(this.king, result);

        return result;
    }

    private void dfs(Person king, List<String> result) {
        if(king != null) {
            if(king.isAlive())
                result.add(king.name());

            for(Person child : king.children())
                dfs(child, result);
        }
    }

    private final class Person {
        private final String name;
        private final List<Person> children;
        private boolean isAlive;

        public Person(final String name) {
            this.name = name;
            this.children = new ArrayList<>();
            this.isAlive = true;
        }

        public String name() {
            return this.name;
        }

        public List<Person> children() {
            return this.children;
        }

        public boolean isAlive() {
            return this.isAlive;
        }

        public void isAlive(final boolean isAlive) {
            this.isAlive = isAlive;
        }
    }
}

