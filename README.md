# HM-Tracker
Home task tracking application

---

## Features

- User management with Firebase Cloud Messaging token support
- Course and task creation with detailed metadata
- Task state machine with valid state transitions (Created, InProgress, Completed, Overdue, Archived)
- Task priority and subject categorization
- Notifications for task deadlines and updates
- RESTful API built with http4s and Circe JSON
- PostgreSQL persistence with Doobie
- Functional programming with Cats Effect

---

## Tech Stack

- **Scala 3.3.1**
- **http4s** for HTTP server
- **Circe** for JSON encoding/decoding
- **Doobie** for PostgreSQL interaction
- **Cats Effect** for effect management
- **PostgreSQL** as the database
- **Firebase Admin SDK** for push notifications

---

## Getting Started

### Prerequisites

- JDK 17 or newer
- PostgreSQL database
- [sbt](https://www.scala-sbt.org/) build tool

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/hm-tracker.git
   cd hm-tracker
