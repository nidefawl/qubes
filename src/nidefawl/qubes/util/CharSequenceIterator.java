package nidefawl.qubes.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */


public class CharSequenceIterator
  implements Iterator<Character>
{
  private final CharSequence sequence;
  private int pos;

  public CharSequenceIterator(CharSequence sequence)
  {
    this.sequence = sequence;
  }

  public boolean hasNext() {
    return this.pos < this.sequence.length();
  }

  public Character next()
  {
    return Character.valueOf(nextChar());
  }

  public char nextChar()
  {
    try {
      return this.sequence.charAt(this.pos++); } catch (IndexOutOfBoundsException exception) {
    }
    throw new NoSuchElementException("Reached end of CharSequence");
  }

  public void remove()
  {
    throw new UnsupportedOperationException("CharSequence objects are immutable");
  }
}