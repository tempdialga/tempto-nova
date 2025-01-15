package com.mygdx.tempto.input;

/**An interface for accepting user input. A priority queue of these will be built, with the highest one getting input, and the rest being told to suppress input.
 * An implementing class should make sure to only consider input when it is being told to consider input, and should consider there to be no input when told to suppress.*/
public interface UserInterface {



}
