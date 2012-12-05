package net.md_5.analyst;

import java.util.Objects;

public class Ownable {

    public String owner;
    public String name;
    public String descriptor;

    public Ownable(String owner, String name, String descriptor) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.owner);
        hash = 47 * hash + Objects.hashCode(this.name);
        hash = 47 * hash + Objects.hashCode(this.descriptor);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Ownable other = (Ownable) obj;
        if (!Objects.equals(this.owner, other.owner)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.descriptor, other.descriptor)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return owner + "/" + name + " " + descriptor;
    }
}
