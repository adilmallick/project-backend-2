import { RestaurantResponse } from "@swiggy/types";

export const MOCK_RESTAURANTS: RestaurantResponse[] = [
  {
    id: "c4b07384-d113-40e1-bbd5-f86749964d7c",
    name: "Punjabi Grill Express",
    description: "Authentic North Indian curries, tandoori breads, and slow-cooked dal makhani.",
    address: "100 Feet Rd, Indiranagar, Bengaluru",
    ownerId: "b1b07384-d113-40e1-bbd5-f86749964d7b",
    isActive: true,
    menuItems: [
      {
        id: "a1b07384-d113-40e1-bbd5-f86749964d7a",
        name: "Paneer Butter Masala",
        description: "Fresh cottage cheese simmered in a rich tomato, butter, and cashew gravy.",
        priceCents: 24900, // ₹249.00
        isAvailable: true,
      },
      {
        id: "a2b07384-d113-40e1-bbd5-f86749964d7b",
        name: "Butter Garlic Naan",
        description: "Clay oven baked flatbread infused with roasted minced garlic and butter.",
        priceCents: 6500, // ₹65.00
        isAvailable: true,
      },
      {
        id: "a3b07384-d113-40e1-bbd5-f86749964d7c",
        name: "Dal Makhani",
        description: "Black lentils slow-cooked overnight with cream, butter, and aromatic spices.",
        priceCents: 21900, // ₹219.00
        isAvailable: true,
      },
    ],
  },
  {
    id: "d5b07384-d113-40e1-bbd5-f86749964d7d",
    name: "Biryani Blues",
    description: "Traditional Hyderabadi Dum Biryani served with spicy salan and creamy raita.",
    address: "Koramangala 5th Block, Bengaluru",
    ownerId: "b2b07384-d113-40e1-bbd5-f86749964d7e",
    isActive: true,
    menuItems: [
      {
        id: "a4b07384-d113-40e1-bbd5-f86749964d7d",
        name: "Hyderabadi Chicken Dum Biryani",
        description: "Long grain basmati rice layered with spiced marinated chicken and saffron.",
        priceCents: 32900, // ₹329.00
        isAvailable: true,
      },
      {
        id: "a5b07384-d113-40e1-bbd5-f86749964d7e",
        name: "Mirchi Ka Salan",
        description: "Spicy and tangy green chili curry cooked in a peanut and sesame seed paste.",
        priceCents: 9900, // ₹99.00
        isAvailable: true,
      },
      {
        id: "a6b07384-d113-40e1-bbd5-f86749964d7f",
        name: "Shahi Tukda",
        description: "Classic royal dessert of ghee fried bread soaked in saffron rabri.",
        priceCents: 12900, // ₹129.00
        isAvailable: true,
      },
    ],
  },
  {
    id: "e6b07384-d113-40e1-bbd5-f86749964d7e",
    name: "Pizza Paradiso & Pasta",
    description: "Wood-fired artisanal Neapolitan pizzas and hand-rolled pasta.",
    address: "Lavelle Road, Central Bengaluru",
    ownerId: "b3b07384-d113-40e1-bbd5-f86749964d7f",
    isActive: true,
    menuItems: [
      {
        id: "a7b07384-d113-40e1-bbd5-f86749964d80",
        name: "Margherita D.O.P.",
        description: "San Marzano tomatoes, buffalo mozzarella, fresh basil, and extra virgin olive oil.",
        priceCents: 39900, // ₹399.00
        isAvailable: true,
      },
      {
        id: "a8b07384-d113-40e1-bbd5-f86749964d81",
        name: "Truffle Mushroom Fettuccine",
        description: "Fresh pasta tossed in wild porcini mushroom sauce with black truffle oil.",
        priceCents: 44900, // ₹449.00
        isAvailable: true,
      },
    ],
  },
  {
    id: "f7b07384-d113-40e1-bbd5-f86749964d7f",
    name: "South Express Tiffins",
    description: "Crispy dosas, fluffy steamed idlis, and filter kaapi.",
    address: "Jayanagar 4th Block, Bengaluru",
    ownerId: "b4b07384-d113-40e1-bbd5-f86749964d82",
    isActive: true,
    menuItems: [
      {
        id: "a9b07384-d113-40e1-bbd5-f86749964d82",
        name: "Ghee Roast Masala Dosa",
        description: "Golden crispy crepe roasted in pure desi ghee, filled with spiced potato mash.",
        priceCents: 11000, // ₹110.00
        isAvailable: true,
      },
      {
        id: "b0b07384-d113-40e1-bbd5-f86749964d83",
        name: "Filter Coffee Decoc",
        description: "Traditional South Indian frothy chicory-blended milk coffee.",
        priceCents: 4500, // ₹45.00
        isAvailable: true,
      },
    ],
  },
];
